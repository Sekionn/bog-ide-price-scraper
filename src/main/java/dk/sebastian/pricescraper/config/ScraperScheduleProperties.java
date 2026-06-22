package dk.sebastian.pricescraper.config;

public class ScraperScheduleProperties {

    private String cron = "0 0 22 ? * MON,WED,FRI";
    private String zone = "Europe/Copenhagen";

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
