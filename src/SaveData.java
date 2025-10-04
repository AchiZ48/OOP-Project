import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    List<PlayerData> players = new ArrayList<>();

    static class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        double x, y;
        Stats stats;
    }

    static SaveData fromParty(List<Player> party) {
        SaveData sd = new SaveData();
        for (Player p : party) {
            PlayerData pd = new PlayerData();
            pd.name = p.name;
            pd.x = p.getPreciseX();
            pd.y = p.getPreciseY();
            pd.stats = p.getStats().copy();
            sd.players.add(pd);
        }
        return sd;
    }

    List<Player> toParty() {
        List<Player> out = new ArrayList<>();
        for (PlayerData pd : players) {
            Player p = Player.createSample(pd.name, pd.x, pd.y);
            if (pd.stats != null) {
                p.applyStats(pd.stats);
            } else {
                p.refreshDerivedStats();
            }
            out.add(p);
        }
        return out;
    }
}

