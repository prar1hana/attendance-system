package com.attendance.system.service;

import com.attendance.system.model.DailyCalendar;
import com.attendance.system.repository.DailyCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CalendarService {

    private final DailyCalendarRepository calendarRepository;
    private final CalendarTemplateService templateService;

    // ===== CALENDAR ACCESS WITH AUTO-GENERATION =====

    /**
     * Get or create calendar - automatically generates base template if not exists
     */
    public DailyCalendar getOrCreateCalendar(String year, String month) {
        log.debug("Getting or creating calendar for {}-{}", year, month);

        validateYearMonth(year, month);

        Optional<DailyCalendar> existing = calendarRepository.findByYearAndMonth(year, month);
        if (existing.isPresent()) {
            log.debug("Found existing calendar for {}-{}", year, month);
            return existing.get();
        }

        // Generate base calendar using template service
        DailyCalendar baseCalendar = templateService.generateBaseCalendar(year, month);
        DailyCalendar savedCalendar = calendarRepository.save(baseCalendar);
        log.info("Created new calendar for {}-{}", year, month);
        return savedCalendar;
    }

    /**
     * Get or create calendar with specific region
     */
    public DailyCalendar getOrCreateCalendar(String year, String month, String regionCode) {
        log.debug("Getting or creating calendar for {}-{} in region {}", year, month, regionCode);

        validateYearMonth(year, month);
        validateRegionCode(regionCode);

        Optional<DailyCalendar> existing = calendarRepository.findByYearAndMonth(year, month);
        if (existing.isPresent()) {
            DailyCalendar calendar = existing.get();
            if (regionCode.equals(calendar.getRegion())) {
                return calendar;
            }
        }

        // Generate base calendar using template service with region
        DailyCalendar baseCalendar = templateService.generateBaseCalendar(year, month, regionCode);
        return calendarRepository.save(baseCalendar);
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Create a new calendar
     */
    public DailyCalendar createCalendar(@Valid DailyCalendar calendar) {
        log.debug("Creating calendar for {}-{}", calendar.getYear(), calendar.getMonth());

        validateCalendarData(calendar);

        // Check if calendar already exists
        if (calendarRepository.existsByYearAndMonth(calendar.getYear(), calendar.getMonth())) {
            throw new IllegalArgumentException("Calendar already exists for " +
                    calendar.getYear() + "-" + calendar.getMonth());
        }

        return calendarRepository.save(calendar);
    }

    /**
     * Generate calendar from template
     */
    public DailyCalendar generateCalendar(String year, String month, List<Integer> holidays) {
        log.debug("Generating calendar for {}-{} with holidays: {}", year, month, holidays);

        validateYearMonth(year, month);

        DailyCalendar calendar = templateService.generateBaseCalendar(year, month);

        // Apply additional holidays if provided
        if (holidays != null && !holidays.isEmpty()) {
            applyAdditionalHolidays(calendar, holidays);
        }

        return calendarRepository.save(calendar);
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Update calendar
     */
    public DailyCalendar updateCalendar(String year, String month, @Valid DailyCalendar calendar) {
        log.debug("Updating calendar for {}-{}", year, month);

        validateYearMonth(year, month);
        validateCalendarData(calendar);

        Optional<DailyCalendar> existingOpt = calendarRepository.findByYearAndMonth(year, month);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Calendar not found for " + year + "-" + month);
        }

        DailyCalendar existing = existingOpt.get();

        // Update fields
        existing.setDays(calendar.getDays());
        existing.setRegion(calendar.getRegion());
        existing.setOrganizationId(calendar.getOrganizationId());

        return calendarRepository.save(existing);
    }

    /**
     * Update day status (type and attendance)
     */
    public DailyCalendar updateDayStatus(String year, String month, int day,
                                         String type, String attendance, String description) {
        log.debug("Updating day status for {}-{}-{} to type: {}, attendance: {}",
                year, month, day, type, attendance);

        DailyCalendar calendar = getOrCreateCalendar(year, month);

        boolean updated = calendar.updateDayStatus(day, type, attendance, description);
        if (!updated) {
            throw new IllegalArgumentException("Day " + day + " not found in calendar");
        }

        // Validate the update
        validateDayUpdate(type, attendance);

        return calendarRepository.save(calendar);
    }

    /**
     * Update only attendance for a working day
     */
    public DailyCalendar updateAttendance(String year, String month, int day, String attendance) {
        log.debug("Updating attendance for {}-{}-{} to {}", year, month, day, attendance);

        // Validate attendance type
        if (attendance != null && !Arrays.asList("wfoffice", "wfh").contains(attendance)) {
            throw new IllegalArgumentException("Invalid attendance type. Must be 'wfoffice' or 'wfh'");
        }

        DailyCalendar calendar = getOrCreateCalendar(year, month);

        boolean updated = calendar.updateAttendance(day, attendance);
        if (!updated) {
            throw new IllegalArgumentException("Cannot set attendance for day " + day +
                    ". Day must exist and be a working day.");
        }

        return calendarRepository.save(calendar);
    }

    /**
     * Update only day type
     */
    public DailyCalendar updateDayType(String year, String month, int day, String type) {
        log.debug("Updating day type for {}-{}-{} to {}", year, month, day, type);

        // Validate day type
        if (!Arrays.asList("working", "holiday", "weekend", "leave").contains(type)) {
            throw new IllegalArgumentException("Invalid day type. Must be 'working', 'holiday', 'weekend', or 'leave'");
        }

        DailyCalendar calendar = getOrCreateCalendar(year, month);
        boolean updated = calendar.updateDayType(day, type);

        if (!updated) {
            throw new IllegalArgumentException("Day " + day + " not found in calendar");
        }

        return calendarRepository.save(calendar);
    }

    /**
     * Add day entry
     */
    public DailyCalendar addDayEntry(String year, String month, DailyCalendar.DayEntry dayEntry) {
        log.debug("Adding day entry for {}-{}-{}", year, month, dayEntry.getDay());

        validateDayEntry(dayEntry);

        DailyCalendar calendar = getOrCreateCalendar(year, month);
        calendar.addDayEntry(dayEntry);

        return calendarRepository.save(calendar);
    }

    // ===== BULK UPDATE OPERATIONS =====

    /**
     * Bulk update multiple days' attendance
     */
    public List<DailyCalendar> bulkUpdateAttendance(String year, String month, Map<Integer, String> attendanceMap) {
        log.debug("Bulk updating attendance for {}-{}", year, month);

        if (attendanceMap == null || attendanceMap.isEmpty()) {
            throw new IllegalArgumentException("Attendance map cannot be null or empty");
        }

        DailyCalendar calendar = getOrCreateCalendar(year, month);

        for (Map.Entry<Integer, String> entry : attendanceMap.entrySet()) {
            int day = entry.getKey();
            String attendance = entry.getValue();

            // Validate attendance type
            if (attendance != null && !Arrays.asList("wfoffice", "wfh").contains(attendance)) {
                log.warn("Invalid attendance type {} for day {}, skipping", attendance, day);
                continue;
            }

            calendar.updateAttendance(day, attendance);
        }

        DailyCalendar savedCalendar = calendarRepository.save(calendar);
        return Arrays.asList(savedCalendar);
    }

    /**
     * Bulk update multiple days' types
     */
    public List<DailyCalendar> bulkUpdateDayTypes(String year, String month, Map<Integer, String> dayTypeMap) {
        log.debug("Bulk updating day types for {}-{}", year, month);

        if (dayTypeMap == null || dayTypeMap.isEmpty()) {
            throw new IllegalArgumentException("Day type map cannot be null or empty");
        }

        DailyCalendar calendar = getOrCreateCalendar(year, month);

        for (Map.Entry<Integer, String> entry : dayTypeMap.entrySet()) {
            int day = entry.getKey();
            String type = entry.getValue();

            // Validate day type
            if (!Arrays.asList("working", "holiday", "weekend", "leave").contains(type)) {
                log.warn("Invalid day type {} for day {}, skipping", type, day);
                continue;
            }

            calendar.updateDayType(day, type);
        }

        DailyCalendar savedCalendar = calendarRepository.save(calendar);
        return Arrays.asList(savedCalendar);
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Delete calendar
     */
    public void deleteCalendar(String year, String month) {
        log.debug("Deleting calendar for {}-{}", year, month);

        validateYearMonth(year, month);

        if (!calendarRepository.existsByYearAndMonth(year, month)) {
            throw new IllegalArgumentException("Calendar not found for " + year + "-" + month);
        }

        calendarRepository.deleteByYearAndMonth(year, month);
        log.info("Deleted calendar for {}-{}", year, month);
    }

    // ===== READ-ONLY OPERATIONS =====

    /**
     * Get day status information
     */
    public Map<String, String> getDayStatus(String year, String month, int day) {
        DailyCalendar calendar = getOrCreateCalendar(year, month);

        Optional<DailyCalendar.DayEntry> dayEntryOpt = calendar.getDayEntry(day);
        if (dayEntryOpt.isEmpty()) {
            throw new IllegalArgumentException("Day " + day + " not found in " + year + "-" + month);
        }

        DailyCalendar.DayEntry dayEntry = dayEntryOpt.get();
        Map<String, String> status = new HashMap<>();
        status.put("day", String.valueOf(day));
        status.put("type", dayEntry.getType());
        status.put("isUpdated", String.valueOf(dayEntry.getIsUpdated()));

        if ("working".equals(dayEntry.getType())) {
            status.put("attendance", dayEntry.getAttendance() != null ? dayEntry.getAttendance() : "not_set");
        } else {
            status.put("attendance", "not_applicable");
        }

        if (dayEntry.getDescription() != null) {
            status.put("description", dayEntry.getDescription());
        }

        return status;
    }

    /**
     * Get all calendars
     */
    public List<DailyCalendar> getAllCalendars() {
        log.debug("Fetching all calendars");
        return calendarRepository.findAll();
    }

    /**
     * Get calendar (without auto-creation)
     */
    public Optional<DailyCalendar> getCalendar(String year, String month) {
        log.debug("Fetching calendar for {}-{}", year, month);
        validateYearMonth(year, month);
        return calendarRepository.findByYearAndMonth(year, month);
    }

    // ===== STATISTICS =====

    public Map<String, Object> getCalendarStatistics(String year, String month) {
        log.debug("Calculating statistics for {}-{}", year, month);
        DailyCalendar calendar = getOrCreateCalendar(year, month);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", calendar.getDays().size());
        stats.put("workingDays", calendar.getWorkingDaysCount());
        stats.put("holidays", calendar.getHolidaysCount());
        stats.put("weekends", calendar.getWeekendsCount());
        stats.put("leaveDays", calendar.getLeaveDaysCount());
        stats.put("officeAttendance", calendar.getOfficeAttendanceCount());
        stats.put("wfhAttendance", calendar.getWfhAttendanceCount());
        stats.put("totalAttendance", calendar.getAttendanceDaysCount());
        stats.put("workingDaysWithoutAttendance", calendar.getWorkingDaysWithoutAttendance());
        stats.put("updatedDays", calendar.getUpdatedDaysCount());
        stats.put("attendanceRate", calendar.getAttendanceRate());
        stats.put("officeAttendanceRate", calendar.getOfficeAttendanceRate());
        stats.put("wfhAttendanceRate", calendar.getWfhAttendanceRate());

        return stats;
    }

    public Map<String, Object> getOverallStatistics() {
        log.debug("Calculating overall statistics");
        List<DailyCalendar> allCalendars = calendarRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCalendars", allCalendars.size());
        stats.put("totalWorkingDays", allCalendars.stream().mapToLong(DailyCalendar::getWorkingDaysCount).sum());
        stats.put("totalHolidays", allCalendars.stream().mapToLong(DailyCalendar::getHolidaysCount).sum());
        stats.put("totalWeekends", allCalendars.stream().mapToLong(DailyCalendar::getWeekendsCount).sum());
        stats.put("totalOfficeAttendance", allCalendars.stream().mapToLong(DailyCalendar::getOfficeAttendanceCount).sum());
        stats.put("totalWfhAttendance", allCalendars.stream().mapToLong(DailyCalendar::getWfhAttendanceCount).sum());

        return stats;
    }

    public Map<String, Object> getYearlyStatistics(String year) {
        log.debug("Calculating yearly statistics for {}", year);
        List<DailyCalendar> yearCalendars = calendarRepository.findByYear(year);

        Map<String, Object> stats = new HashMap<>();
        stats.put("year", year);
        stats.put("totalCalendars", yearCalendars.size());
        stats.put("totalWorkingDays", yearCalendars.stream().mapToLong(DailyCalendar::getWorkingDaysCount).sum());
        stats.put("totalHolidays", yearCalendars.stream().mapToLong(DailyCalendar::getHolidaysCount).sum());
        stats.put("totalWeekends", yearCalendars.stream().mapToLong(DailyCalendar::getWeekendsCount).sum());
        stats.put("totalOfficeAttendance", yearCalendars.stream().mapToLong(DailyCalendar::getOfficeAttendanceCount).sum());
        stats.put("totalWfhAttendance", yearCalendars.stream().mapToLong(DailyCalendar::getWfhAttendanceCount).sum());

        return stats;
    }

    // ===== QUERY METHODS =====

    public List<DailyCalendar> getCalendarsByYear(String year) {
        validateYear(year);
        return calendarRepository.findByYear(year);
    }

    public List<DailyCalendar> getCalendarsWithHolidays() {
        return calendarRepository.findCalendarsWithHolidays();
    }

    public List<DailyCalendar> getCalendarsWithAttendance() {
        return calendarRepository.findCalendarsWithAttendance();
    }

    public List<DailyCalendar> getCalendarsWithOfficeAttendance() {
        return calendarRepository.findCalendarsWithOfficeAttendance();
    }

    public List<DailyCalendar> getCalendarsWithWfhAttendance() {
        return calendarRepository.findCalendarsWithWfhAttendance();
    }

    public List<DailyCalendar> getCalendarsWithMixedAttendance() {
        return calendarRepository.findCalendarsWithMixedAttendance();
    }

    public List<DailyCalendar> getCalendarsWithIncompleteAttendance() {
        return calendarRepository.findCalendarsWithIncompleteAttendance();
    }

    public List<DailyCalendar> getCalendarsWithFullAttendance() {
        return calendarRepository.findCalendarsWithFullAttendance();
    }

    public List<DailyCalendar> getCalendarsByDateRange(String startYear, String startMonth, String endYear, String endMonth) {
        validateYearMonth(startYear, startMonth);
        validateYearMonth(endYear, endMonth);
        return calendarRepository.findByDateRange(startYear, startMonth, endYear, endMonth);
    }

    // ===== UTILITY METHODS =====

    public boolean calendarExists(String year, String month) {
        validateYearMonth(year, month);
        return calendarRepository.existsByYearAndMonth(year, month);
    }

    public long getTotalCalendarsCount() {
        return calendarRepository.count();
    }

    public List<String> getDistinctYears() {
        return calendarRepository.findDistinctYears();
    }

    public long getCalendarCountByYear(String year) {
        validateYear(year);
        return calendarRepository.countByYear(year);
    }

    // ===== PRIVATE VALIDATION METHODS =====

    public void validateCalendarData(DailyCalendar calendar) {
        if (calendar == null) {
            throw new IllegalArgumentException("Calendar cannot be null");
        }

        validateYearMonth(calendar.getYear(), calendar.getMonth());

        if (calendar.getDays() == null) {
            throw new IllegalArgumentException("Calendar days cannot be null");
        }

        // Validate each day entry
        for (DailyCalendar.DayEntry day : calendar.getDays()) {
            validateDayEntry(day);
        }
    }

    private void validateDayEntry(DailyCalendar.DayEntry dayEntry) {
        if (dayEntry == null) {
            throw new IllegalArgumentException("Day entry cannot be null");
        }

        if (dayEntry.getDay() < 1 || dayEntry.getDay() > 31) {
            throw new IllegalArgumentException("Day must be between 1 and 31");
        }

        if (dayEntry.getType() == null ||
                !Arrays.asList("working", "holiday", "weekend", "leave").contains(dayEntry.getType())) {
            throw new IllegalArgumentException("Invalid day type");
        }

        if (dayEntry.getAttendance() != null &&
                !Arrays.asList("wfoffice", "wfh").contains(dayEntry.getAttendance())) {
            throw new IllegalArgumentException("Invalid attendance type");
        }

        if (!"working".equals(dayEntry.getType()) && dayEntry.getAttendance() != null) {
            throw new IllegalArgumentException("Attendance can only be set for working days");
        }
    }

    private void validateDayUpdate(String type, String attendance) {
        if (!"working".equals(type) && attendance != null) {
            throw new IllegalArgumentException("Attendance can only be set for working days");
        }

        if (attendance != null && !Arrays.asList("wfoffice", "wfh").contains(attendance)) {
            throw new IllegalArgumentException("Invalid attendance type");
        }
    }

    private void validateYearMonth(String year, String month) {
        validateYear(year);
        validateMonth(month);
    }

    private void validateYear(String year) {
        if (year == null || !year.matches("\\d{4}")) {
            throw new IllegalArgumentException("Year must be 4 digits");
        }
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("^(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("Month must be 01-12");
        }
    }

    private void validateRegionCode(String regionCode) {
        if (regionCode == null || regionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Region code cannot be null or empty");
        }
    }

    private void applyAdditionalHolidays(DailyCalendar calendar, List<Integer> holidays) {
        for (Integer holiday : holidays) {
            if (holiday >= 1 && holiday <= 31) {
                Optional<DailyCalendar.DayEntry> dayEntryOpt = calendar.getDayEntry(holiday);
                if (dayEntryOpt.isPresent()) {
                    DailyCalendar.DayEntry dayEntry = dayEntryOpt.get();
                    if (!"holiday".equals(dayEntry.getType())) {
                        calendar.updateDayType(holiday, "holiday");
                    }
                }
            }
        }
    }
}