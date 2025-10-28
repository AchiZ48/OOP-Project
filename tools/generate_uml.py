import re
from pathlib import Path
from typing import Dict, List, Optional, Tuple


ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = ROOT / "src"
OUTPUT_PATH = ROOT / "docs" / "uml" / "project-class-diagram.puml"

CLASS_PATTERN = re.compile(
    r"""
    ^(?P<modifiers>(?:\s*(?:public|protected|private|abstract|final|static|sealed|non-sealed)\s+)*)?
    (?P<kind>class|interface|enum)\s+
    (?P<name>[A-Za-z_][A-Za-z0-9_]*)
    (?P<rest>[^{]*)
    """,
    re.MULTILINE | re.VERBOSE,
)

VISIBILITY_SYMBOL = {
    "public": "+",
    "protected": "#",
    "private": "-",
}

MEMBER_MODIFIERS = {
    "public",
    "protected",
    "private",
    "static",
    "final",
    "abstract",
    "transient",
    "volatile",
    "synchronized",
    "native",
    "strictfp",
    "default",
}

COMMENT_LINE_RE = re.compile(r"//.*?$", re.MULTILINE)
COMMENT_BLOCK_RE = re.compile(r"/\*.*?\*/", re.DOTALL)


def strip_comments(code: str) -> str:
    without_block = COMMENT_BLOCK_RE.sub("", code)
    without_line = COMMENT_LINE_RE.sub("", without_block)
    return without_line


def sanitize_type_reference(raw: str) -> str:
    raw = raw.strip()
    if not raw:
        return ""
    no_generics = re.sub(r"<[^>]+>", "", raw)
    clean = no_generics.strip()
    clean = clean.rstrip("&")
    if "." in clean:
        clean = clean.split(".")[-1]
    clean = clean.replace("[]", "[]")
    clean = clean.replace("...", "")
    return clean


def simplify_type(raw: str) -> str:
    raw = raw.strip()
    if not raw:
        return ""
    raw = re.sub(r"\s+", " ", raw)
    # remove package prefixes `foo.Bar` -> `Bar`
    raw = re.sub(r"(?<!\.)\b([A-Za-z_]\w*)\.", "", raw)
    return raw


def remove_leading_annotations(text: str) -> str:
    stripped = text.lstrip()
    while stripped.startswith("@"):
        match = re.match(r"^@\w+(?:\s*\([^)]*\))?\s*", stripped)
        if not match:
            break
        stripped = stripped[match.end():].lstrip()
    return stripped


def find_class_body(clean_text: str, start_index: int) -> Tuple[str, int]:
    brace_index = clean_text.find("{", start_index)
    if brace_index == -1:
        return "", -1
    depth = 1
    i = brace_index + 1
    while i < len(clean_text) and depth > 0:
        ch = clean_text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        i += 1
    end_index = i
    body = clean_text[brace_index + 1 : end_index - 1]
    return body, end_index


def split_top_level_members(body: str) -> List[Tuple[str, str]]:
    members: List[Tuple[str, str]] = []
    token: List[str] = []
    depth = 0
    i = 0
    length = len(body)
    while i < length:
        ch = body[i]
        if depth == 0:
            if ch == "{":
                header = "".join(token).strip()
                if header:
                    members.append(("block", header))
                token.clear()
                depth += 1
            elif ch == ";":
                header = "".join(token).strip()
                if header:
                    members.append(("declaration", header))
                token.clear()
            else:
                token.append(ch)
        else:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
        i += 1
    return members


def extract_visibility(modifiers: List[str]) -> str:
    for keyword in ("public", "protected", "private"):
        if keyword in modifiers:
            return VISIBILITY_SYMBOL[keyword]
    return "~"


def clean_modifiers_and_rest(parts: List[str]) -> Tuple[List[str], List[str]]:
    modifiers: List[str] = []
    while parts and parts[0] in MEMBER_MODIFIERS:
        modifiers.append(parts.pop(0))
    return modifiers, parts


def parse_field(header: str) -> Optional[Dict[str, str]]:
    header = remove_leading_annotations(header)
    if not header:
        return None
    left = header.split("=", 1)[0].strip()
    if not left:
        return None
    left = left.strip()
    depth = 0
    trimmed = []
    for ch in left:
        if ch == "<":
            depth += 1
        elif ch == ">":
            if depth > 0:
                depth -= 1
        elif ch == "," and depth == 0:
            break
        trimmed.append(ch)
    left = "".join(trimmed).strip()
    parts = left.split()
    if not parts:
        return None
    modifiers, remainder = clean_modifiers_and_rest(parts)
    if not remainder:
        return None
    name = remainder[-1].rstrip("[]")
    type_part = " ".join(remainder[:-1]).strip()
    if not type_part and remainder:
        type_part = remainder[0]
    visibility = extract_visibility(modifiers)
    prefixes = []
    if "static" in modifiers:
        prefixes.append("{static}")
    if "final" in modifiers:
        prefixes.append("{final}")
    prefix = (" ".join(prefixes) + " ") if prefixes else ""
    return {
        "name": name,
        "type": simplify_type(type_part),
        "visibility": visibility,
        "prefix": prefix,
    }


def parse_parameters(raw_params: str) -> List[str]:
    params = []
    if not raw_params.strip():
        return params
    for fragment in raw_params.split(","):
        fragment = remove_leading_annotations(fragment.strip())
        if not fragment:
            continue
        tokens = fragment.split()
        if not tokens:
            continue
        name = tokens[-1]
        type_part = " ".join(tokens[:-1]).strip()
        if not type_part:
            type_part = name
            name = ""
        entry = f"{name}: {simplify_type(type_part)}" if name else simplify_type(type_part)
        params.append(entry)
    return params


def parse_method(header: str, class_name: str) -> Optional[Dict[str, str]]:
    header = remove_leading_annotations(header)
    if not header or "(" not in header:
        return None
    before_params, _, after = header.partition("(")
    params_part, _, _ = after.partition(")")
    working = before_params.strip()
    if not working:
        return None
    parts = working.split()
    modifiers, remainder = clean_modifiers_and_rest(parts)
    if not remainder:
        return None
    name = remainder[-1]
    return_type = " ".join(remainder[:-1]).strip()
    is_constructor = name == class_name and not return_type
    if not is_constructor and not return_type:
        return None
    visibility = extract_visibility(modifiers)
    prefixes = []
    if "static" in modifiers:
        prefixes.append("{static}")
    if "abstract" in modifiers:
        prefixes.append("{abstract}")
    prefix = (" ".join(prefixes) + " ") if prefixes else ""
    params = parse_parameters(params_part)
    return {
        "name": name,
        "visibility": visibility,
        "params": params,
        "return": simplify_type(return_type) if not is_constructor else "",
        "constructor": is_constructor,
        "prefix": prefix,
    }


def extract_types():
    types = {}
    for path in sorted(SRC_DIR.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        cleaned = strip_comments(text)
        for match in CLASS_PATTERN.finditer(cleaned):
            start_char = match.start()
            if start_char > 0 and cleaned[start_char - 1] not in {"\n", "\r"}:
                continue

            modifiers = (match.group("modifiers") or "").strip().split()
            modifiers = [m for m in modifiers if m]
            kind = match.group("kind")
            name = match.group("name")
            rest = match.group("rest") or ""

            extends_match = re.search(
                r"extends\s+([^{]+?)(?=\s+implements|\s*\{|\s*$)", rest
            )
            implements_match = re.search(
                r"implements\s+([^{]+?)(?=\s*\{|\s*$)", rest
            )

            extends = []
            implements = []

            if extends_match:
                extends_text = extends_match.group(1)
                extends = [
                    sanitize_type_reference(part)
                    for part in extends_text.split(",")
                    if sanitize_type_reference(part)
                ]

            if implements_match:
                implements_text = implements_match.group(1)
                implements = [
                    sanitize_type_reference(part)
                    for part in implements_text.split(",")
                    if sanitize_type_reference(part)
                ]

            body, _ = find_class_body(cleaned, match.end())
            members = split_top_level_members(body) if body else []

            fields = []
            methods = []
            if kind != "enum":
                for member_type, header in members:
                    if member_type == "declaration":
                        field = parse_field(header)
                        if field:
                            fields.append(field)
                    elif member_type == "block":
                        stripped = header.strip()
                        if stripped.startswith(("class ", "interface ", "enum ")):
                            continue
                        if stripped == "static":
                            continue
                        method = parse_method(header, name)
                        if method:
                            methods.append(method)

            types[name] = {
                "path": path,
                "kind": kind,
                "modifiers": modifiers,
                "extends": extends,
                "implements": implements,
                "fields": fields,
                "methods": methods,
            }

    return types


def build_plantuml(types):
    lines = [
        "@startuml",
        "skinparam classAttributeIconSize 0",
    ]

    externals = {}

    def ensure_external(type_name: str):
        if type_name and type_name not in types and type_name not in externals:
            externals[type_name] = None

    for name, info in sorted(types.items()):
        keyword = info["kind"]
        if keyword == "class" and "abstract" in info["modifiers"]:
            keyword = "abstract class"
        members_lines: List[str] = []
        for field in info["fields"]:
            member_line = f"    {field['visibility']} {field['prefix']}{field['name']}"
            if field["type"]:
                member_line += f" : {field['type']}"
            members_lines.append(member_line)
        for method in info["methods"]:
            params = ", ".join(method["params"])
            signature = f"{method['name']}({params})"
            if not method["constructor"] and method["return"]:
                signature += f" : {method['return']}"
            member_line = f"    {method['visibility']} {method['prefix']}{signature}"
            members_lines.append(member_line)
        if members_lines:
            lines.append(f"{keyword} {name} {{")
            lines.extend(members_lines)
            lines.append("}")
        else:
            lines.append(f"{keyword} {name}")

    for name, info in sorted(types.items()):
        for base in info["extends"]:
            ensure_external(base)
            lines.append(f"{name} --|> {base}")
        for iface in info["implements"]:
            ensure_external(iface)
            lines.append(f"{name} ..|> {iface}")

    for name in sorted(externals):
        lines.append(f"class {name} <<external>>")

    lines.append("@enduml")
    return "\n".join(lines)


def main():
    types = extract_types()
    plantuml_text = build_plantuml(types)
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(plantuml_text, encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
