import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    List<PlayerData> players = new ArrayList<>();

    static class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int x, y;
        int hp, maxHp, level, exp, str, def;
    }

    static SaveData fromParty(List<Player> party) {
        SaveData sd = new SaveData();
        for (Player p : party) {
            PlayerData pd = new PlayerData();
            pd.name = p.name;
            pd.x = p.x;
            pd.y = p.y;
            pd.hp = p.hp;
            pd.maxHp = p.maxHp;
            pd.level = p.level;
            pd.exp = p.exp;
            pd.str = p.str;
            pd.def = p.def;
            sd.players.add(pd);
        }
        return sd;
    }

    List<Player> toParty() {
        List<Player> out = new ArrayList<>();
        for (PlayerData pd : players) {
            Player p = Player.createSample(pd.name, pd.x, pd.y);
            p.hp = pd.hp;
            p.maxHp = pd.maxHp;
            p.level = pd.level;
            p.exp = pd.exp;
            p.str = pd.str;
            p.def = pd.def;
            out.add(p);
        }
        return out;
    }
}
