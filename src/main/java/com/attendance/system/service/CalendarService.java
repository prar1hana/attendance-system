package com.attendance.system.service;
import jakarta.persistence.EntityNotFoundException;

import com.attendance.system.model.DailyCalendar;
import com.attendance.system.repository.DailyCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CalendarService {

    private final DailyCalendarRepository calendarRepository;

    // ===== BASIC CRUD OPERATIONS =====
    public Map<String, String> getDayStatus(String year, String month, int day) {
        DailyCalendar calendar = calendarRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calendar not found for " + year + "-" + month
                ));

        DailyCalendar.DayEntry dayEntry = calendar.getDays().stream()
                .filter(d -> d.getDay() == day)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Day " + day + " not found in " + year + "-" + month
                ));

        Map<String, String> status = new HashMap<>();
        status.put("day", String.valueOf(day));
        status.put("type", dayEntry.getType());

        if ("working".equals(dayEntry.getType())) {
            status.put("attendance",
                    dayEntry.getAttendance() != null ? dayEntry.getAttendance() : "absent"
            );
        } else {
            status.put("attendance", "non-working");
        }

        return status;
    }

    public List<DailyCalendar> getAllCalendars() {
        log.debug("Fetching all calendars");
        return calendarRepository.findAll();
    }

    public Optional<DailyCalendar> getCalendar(String year, String month) {
        log.debug("Fetching calendar for {}-{}", year, month);
        return calendarRepository.findByYearAndMonth(year, month);
    }

    public DailyCalendar createCalendar(DailyCalendar calendar) {
        log.debug("Creating calendar for {}-{}", calendar.getYear(), calendar.getMonth());
        if (calendarRepository.existsByYearAndMonth(calendar.getYear(), calendar.getMonth())) {
            throw new RuntimeException("Calendar already exists for " + calendar.getYear() + "-" + calendar.getMonth());
        }
        return calendarRepository.save(calendar);
    }

    public DailyCalendar updateCalendar(String year, String month, DailyCalendar calendar) {
        log.debug("Updating calendar for {}-{}", year, month);
        Optional<DailyCalendar> existingCalendar = calendarRepository.findByYearAndMonth(year, month);
        if (existingCalendar.isPresent()) {
            DailyCalendar existing = existingCalendar.get();
            existing.setDays(calendar.getDays());
            return calendarRepository.save(existing);
        } else {
            calendar.setYear(year);
            calendar.setMonth(month);
            return calendarRepository.save(calendar);
        }
    }

    public void deleteCalendar(String year, String month) {
        log.debug("Deleting calendar for {}-{}", year, month);
        calendarRepository.deleteByYearAndMonth(year, month);
    }

    // ===== CALENDAR GENERATION =====

    public DailyCalendar generateCalendar(String year, String month, List<Integer> holidays) {
        log.debug("Generating calendar for {}-{} with holidays: {}", year, month, holidays);
        int yearInt = Integer.parseInt(year);
        int monthInt = Integer.parseInt(month);
        YearMonth yearMonth = YearMonth.of(yearInt, monthInt);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<DailyCalendar.DayEntry> days = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            String dayType = getDayType(yearInt, monthInt, day, holidays);
            days.add(new DailyCalendar.DayEntry(day, dayType, null));
        }

        DailyCalendar calendar = new DailyCalendar(year, month, days);
        return calendarRepository.save(calendar);
    }

    // ===== ATTENDANCE MANAGEMENT =====

    public DailyCalendar updateAttendance(String year, String month, int day, String attendance) {
        log.debug("Updating attendance for {}-{}-{} to {}", year, month, day, attendance);

        // Validate attendance type
        if (attendance != null && !Arrays.asList("wfoffice", "wfh").contains(attendance)) {
            throw new IllegalArgumentException("Invalid attendance type. Must be 'wfoffice' or 'wfh'");
        }

        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();
        DailyCalendar.DayEntry dayEntry = calendar.getDayEntry(day);

        if (dayEntry == null) {
            throw new RuntimeException("Day " + day + " not found in calendar");
        }

        if (!"working".equals(dayEntry.getType())) {
            throw new RuntimeException("Cannot set attendance for non-working day");
        }

        dayEntry.setAttendance(attendance);
        return calendarRepository.save(calendar);
    }

    public DailyCalendar addDayEntry(String year, String month, DailyCalendar.DayEntry dayEntry) {
        log.debug("Adding day entry for {}-{}-{}", year, month, dayEntry.getDay());
        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();
        calendar.addDayEntry(dayEntry);
        return calendarRepository.save(calendar);
    }

    public DailyCalendar updateDayType(String year, String month, int day, String type) {
        log.debug("Updating day type for {}-{}-{} to {}", year, month, day, type);

        // Validate day type
        if (!Arrays.asList("working", "holiday", "weekend").contains(type)) {
            throw new IllegalArgumentException("Invalid day type. Must be 'working', 'holiday', or 'weekend'");
        }

        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();
        DailyCalendar.DayEntry dayEntry = calendar.getDayEntry(day);

        if (dayEntry == null) {
            throw new RuntimeException("Day " + day + " not found in calendar");
        }

        dayEntry.setType(type);
        if (!"working".equals(type)) {
            dayEntry.setAttendance(null); // Clear attendance for non-working days
        }

        return calendarRepository.save(calendar);
    }

    // ===== QUERY METHODS =====

    public List<DailyCalendar> getCalendarsByYear(String year) {
        log.debug("Fetching calendars for year {}", year);
        return calendarRepository.findByYear(year);
    }

    public List<DailyCalendar> getCalendarsWithHolidays() {
        log.debug("Fetching calendars with holidays");
        return calendarRepository.findCalendarsWithHolidays();
    }

    public List<DailyCalendar> getCalendarsWithAttendance() {
        log.debug("Fetching calendars with attendance data");
        return calendarRepository.findCalendarsWithAttendance();
    }

    public List<DailyCalendar> getCalendarsByAttendanceType(String attendanceType) {
        log.debug("Fetching calendars with attendance type: {}", attendanceType);
        return calendarRepository.findByAttendanceType(attendanceType);
    }

    public List<DailyCalendar> getCalendarsWithOfficeAttendance() {
        log.debug("Fetching calendars with office attendance");
        return calendarRepository.findCalendarsWithOfficeAttendance();
    }

    public List<DailyCalendar> getCalendarsWithWfhAttendance() {
        log.debug("Fetching calendars with WFH attendance");
        return calendarRepository.findCalendarsWithWfhAttendance();
    }

    public List<DailyCalendar> getCalendarsWithMixedAttendance() {
        log.debug("Fetching calendars with mixed attendance");
        return calendarRepository.findCalendarsWithMixedAttendance();
    }

    public List<DailyCalendar> getCalendarsByDateRange(String startYear, String startMonth, String endYear, String endMonth) {
        log.debug("Fetching calendars from {}-{} to {}-{}", startYear, startMonth, endYear, endMonth);
        return calendarRepository.findByDateRange(startYear, startMonth, endYear, endMonth);
    }

    public List<DailyCalendar> getCalendarsWithIncompleteAttendance() {
        log.debug("Fetching calendars with incomplete attendance");
        return calendarRepository.findCalendarsWithIncompleteAttendance();
    }

    public List<DailyCalendar> getCalendarsWithFullAttendance() {
        log.debug("Fetching calendars with full attendance");
        return calendarRepository.findCalendarsWithFullAttendance();
    }

    // ===== STATISTICS =====

    public Map<String, Object> getCalendarStatistics(String year, String month) {
        log.debug("Calculating statistics for {}-{}", year, month);
        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalDays", calendar.getDays().size());
        stats.put("workingDays", calendar.getWorkingDaysCount());
        stats.put("holidays", calendar.getHolidaysCount());
        stats.put("weekends", calendar.getWeekendsCount());
        stats.put("officeAttendance", calendar.getOfficeAttendanceCount());
        stats.put("wfhAttendance", calendar.getWfhAttendanceCount());
        stats.put("totalAttendance", calendar.getAttendanceDaysCount());
        stats.put("workingDaysWithoutAttendance", calendar.getWorkingDaysWithoutAttendance());
        stats.put("attendanceRate", calendar.getAttendanceRate());
        stats.put("officeAttendanceRate", calendar.getOfficeAttendanceRate());
        stats.put("wfhAttendanceRate", calendar.getWfhAttendanceRate());

        return stats;
    }

    public Map<String, Object> getOverallStatistics() {
        log.debug("Calculating overall statistics");
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCalendars", calendarRepository.count());
        stats.put("totalWorkingDays", calendarRepository.countWorkingDays());
        stats.put("totalHolidays", calendarRepository.countHolidays());
        stats.put("totalWeekends", calendarRepository.countWeekends());
        stats.put("totalOfficeAttendance", calendarRepository.countOfficeAttendanceDays());
        stats.put("totalWfhAttendance", calendarRepository.countWfhAttendanceDays());

        return stats;
    }

    public Map<String, Object> getYearlyStatistics(String year) {
        log.debug("Calculating yearly statistics for {}", year);
        List<DailyCalendar> yearCalendars = calendarRepository.findByYear(year);

        Map<String, Object> stats = new HashMap<>();
        stats.put("year", year);
        stats.put("totalMonths", yearCalendars.size());

        long totalWorkingDays = yearCalendars.stream().mapToLong(DailyCalendar::getWorkingDaysCount).sum();
        long totalHolidays = yearCalendars.stream().mapToLong(DailyCalendar::getHolidaysCount).sum();
        long totalWeekends = yearCalendars.stream().mapToLong(DailyCalendar::getWeekendsCount).sum();
        long totalOfficeAttendance = yearCalendars.stream().mapToLong(DailyCalendar::getOfficeAttendanceCount).sum();
        long totalWfhAttendance = yearCalendars.stream().mapToLong(DailyCalendar::getWfhAttendanceCount).sum();

        stats.put("totalWorkingDays", totalWorkingDays);
        stats.put("totalHolidays", totalHolidays);
        stats.put("totalWeekends", totalWeekends);
        stats.put("totalOfficeAttendance", totalOfficeAttendance);
        stats.put("totalWfhAttendance", totalWfhAttendance);

        if (totalWorkingDays > 0) {
            stats.put("yearlyAttendanceRate", (double)(totalOfficeAttendance + totalWfhAttendance) / totalWorkingDays);
            stats.put("yearlyOfficeRate", (double)totalOfficeAttendance / totalWorkingDays);
            stats.put("yearlyWfhRate", (double)totalWfhAttendance / totalWorkingDays);
        }

        return stats;
    }

    // ===== BULK OPERATIONS =====

    public List<DailyCalendar> bulkUpdateAttendance(String year, String month, Map<Integer, String> attendanceMap) {
        log.debug("Bulk updating attendance for {}-{}", year, month);
        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();

        for (Map.Entry<Integer, String> entry : attendanceMap.entrySet()) {
            int day = entry.getKey();
            String attendance = entry.getValue();

            // Validate attendance type
            if (attendance != null && !Arrays.asList("wfoffice", "wfh").contains(attendance)) {
                log.warn("Invalid attendance type {} for day {}, skipping", attendance, day);
                continue;
            }

            DailyCalendar.DayEntry dayEntry = calendar.getDayEntry(day);
            if (dayEntry != null && "working".equals(dayEntry.getType())) {
                dayEntry.setAttendance(attendance);
            }
        }

        return Arrays.asList(calendarRepository.save(calendar));
    }

    public List<DailyCalendar> bulkUpdateDayTypes(String year, String month, Map<Integer, String> dayTypeMap) {
        log.debug("Bulk updating day types for {}-{}", year, month);
        Optional<DailyCalendar> calendarOpt = calendarRepository.findByYearAndMonth(year, month);
        if (calendarOpt.isEmpty()) {
            throw new RuntimeException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar calendar = calendarOpt.get();

        for (Map.Entry<Integer, String> entry : dayTypeMap.entrySet()) {
            int day = entry.getKey();
            String type = entry.getValue();

            // Validate day type
            if (!Arrays.asList("working", "holiday", "weekend").contains(type)) {
                log.warn("Invalid day type {} for day {}, skipping", type, day);
                continue;
            }

            DailyCalendar.DayEntry dayEntry = calendar.getDayEntry(day);
            if (dayEntry != null) {
                dayEntry.setType(type);
                if (!"working".equals(type)) {
                    dayEntry.setAttendance(null); // Clear attendance for non-working days
                }
            }
        }

        return Arrays.asList(calendarRepository.save(calendar));
    }

    // ===== UTILITY METHODS =====

    private String getDayType(int year, int month, int day, List<Integer> holidays) {
        // Check if it's a holiday
        if (holidays != null && holidays.contains(day)) {
            return "holiday";
        }

        // Check if it's a weekend (Saturday = 7, Sunday = 1)
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day); // Month is 0-based in Calendar
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return "weekend";
        }

        return "working";
    }

    public boolean calendarExists(String year, String month) {
        return calendarRepository.existsByYearAndMonth(year, month);
    }

    public long getTotalCalendarsCount() {
        return calendarRepository.count();
    }

    public List<String> getDistinctYears() {
        return calendarRepository.findDistinctYears();
    }

    public long getCalendarCountByYear(String year) {
        return calendarRepository.countByYear(year);
    }

    // ===== VALIDATION METHODS =====

    public void validateCalendarData(DailyCalendar calendar) {
        log.debug("Validating calendar data for {}-{}", calendar.getYear(), calendar.getMonth());

        // Check for duplicate days
        Set<Integer> daySet = new HashSet<>();
        for (DailyCalendar.DayEntry day : calendar.getDays()) {
            if (!daySet.add(day.getDay())) {
                throw new IllegalArgumentException("Duplicate day found: " + day.getDay());
            }
        }

        // Check for valid day range
        int yearInt = Integer.parseInt(calendar.getYear());
        int monthInt = Integer.parseInt(calendar.getMonth());
        YearMonth yearMonth = YearMonth.of(yearInt, monthInt);
        int maxDays = yearMonth.lengthOfMonth();

        for (DailyCalendar.DayEntry day : calendar.getDays()) {
            if (day.getDay() < 1 || day.getDay() > maxDays) {
                throw new IllegalArgumentException("Invalid day " + day.getDay() + " for month " + calendar.getMonth());
            }
        }

        // Check attendance is only set for working days
        for (DailyCalendar.DayEntry day : calendar.getDays()) {
            if (day.getAttendance() != null && !"working".equals(day.getType())) {
                throw new IllegalArgumentException("Attendance cannot be set for non-working day: " + day.getDay());
            }
        }
    }
}