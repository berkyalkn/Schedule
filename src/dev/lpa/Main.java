package dev.lpa;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.format.DateTimeFormatter.*;

public class Main {

    private record Employee(String name, Locale locale, ZoneId zone){

        public Employee(String name, String locale, String zone) {
            this(name, Locale.forLanguageTag(locale), ZoneId.of(zone));
        }

        public Employee(String name, Locale locale, String zone) {
            this(name, locale, ZoneId.of(zone));
        }

        String getDateInfo(ZonedDateTime ztd, DateTimeFormatter dtf){
            return "%s [%s] : %s".formatted(name, zone, ztd.format(dtf.localizedBy(locale)));
        }
    }

    public static void main(String[] args) {

        Employee berkay = new Employee("Berkay", Locale.US, "America/New_York");
        Employee hakan = new Employee("Hakan", "en-AU", "Australia/Sydney");

        ZoneRules berkayRules = berkay.zone.getRules();
        ZoneRules hakanRules = hakan.zone.getRules();
        System.out.println(berkay + " " + berkayRules);
        System.out.println(hakan + " " + hakanRules);

        ZonedDateTime berkayNow = ZonedDateTime.now(berkay.zone);
        ZonedDateTime hakanNow = ZonedDateTime.of(berkayNow.toLocalDateTime(), hakan.zone);
        long hoursBetween = Duration.between(berkayNow, hakanNow).toHours();
        long minutesBetween = Duration.between(berkayNow, hakanNow).toMinutesPart();
        System.out.println("Berkay is " + Math.abs(hoursBetween) + " hours " +
                Math.abs(minutesBetween) + " minutes " +
                        ((hoursBetween < 0 ) ? "behind " : "ahead"));

        System.out.println("Berkay in daylight savings ? " +
                berkayRules.isDaylightSavings(berkayNow.toInstant()) + " " +
                berkayRules.getDaylightSavings(berkayNow.toInstant()) + ": " +
                berkayNow.format(ofPattern("zzz z")));

        System.out.println("Hakan in daylight savings ? " +
                hakanRules.isDaylightSavings(hakanNow.toInstant()) + " " +
                hakanRules.getDaylightSavings(hakanNow.toInstant()) + ": " +
                hakanNow.format(ofPattern("zzz z")));

        int days = 10;
        var map = schedule(berkay, hakan, days);
        DateTimeFormatter dtf = ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT);

        for (LocalDate ldt : map.keySet()){
            System.out.println(ldt.format(ofLocalizedDate(FormatStyle.FULL)));
            for (ZonedDateTime zdt : map.get(ldt)){
                System.out.println("\t" +
                hakan.getDateInfo(zdt, dtf) + " <----> " +
                berkay.getDateInfo(zdt.withZoneSameInstant(berkay.zone), dtf));
            }
        }
    }

    private static Map<LocalDate, List<ZonedDateTime>> schedule(Employee first,
                                                                Employee second,
                                                                int days){

        Predicate<ZonedDateTime> rules = zdt ->
                zdt.getDayOfWeek() != DayOfWeek.SATURDAY
                && zdt.getDayOfWeek() != DayOfWeek.SUNDAY
                && zdt.getHour() >= 7 && zdt.getHour() < 21;

        LocalDate startingDate = LocalDate.now().plusDays(2);

        return startingDate.datesUntil(startingDate.plusDays(days + 1))
                .map(dt -> dt.atStartOfDay(first.zone))
                .flatMap(dt -> IntStream.range(0, 24).mapToObj(dt::withHour))
                .filter(rules)
                .map(dtz -> dtz.withZoneSameInstant(second.zone))
                .filter(rules)
                .collect(
                        Collectors.groupingBy(ZonedDateTime::toLocalDate,
                                TreeMap::new, Collectors.toList()));
    }
}
