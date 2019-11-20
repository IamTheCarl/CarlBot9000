package net.artifactgaming.carlbot.modules.statistics;

/***
 * Statistic settings for a guild
 */
public class StatisticsSettings {

    ///region SQL Column Names
    public static final String IS_ENABLED = "IS_ENABLED";

    ///endregion

    private boolean isEnabled;

    public StatisticsSettings(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public StatisticsSettings(){
        isEnabled = false;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void setEnabledByString(String enabled){
        isEnabled = enabled.equals("YES");
    }

    public String isEnabledToString(){
        if (isEnabled) {
            return "YES";
        } else {
            return "NO";
        }
    }
}
