package com.attendance.system.controller;

import com.attendance.system.model.DailyCalendar;
import com.attendance.system.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // ===== BASIC CRUD ENDPOINTS =====
    @GetMapping("/{year}/{month}/day/{day}/status")
    public ResponseEntity<Map<String, String>> getDayStatus(
            @PathVariable @Pattern(regexp = "\\d{4}") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$") String month,
            @PathVariable @Min(1) @Max(31) int day) {

        Map<String, String> status = calendarService.getDayStatus(year, month, day);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DailyCalendar>> getAllCalendars() {
        log.info("REST request to get all calendars");
        return ResponseEntity.ok(calendarService.getAllCalendars());
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<DailyCalendar> getCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to get calendar for {}-{}", year, month);
        Optional<DailyCalendar> calendar = calendarService.getCalendar(year, month);
        return calendar.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DailyCalendar> createCalendar(@Valid @RequestBody DailyCalendar calendar) {
        log.info("REST request to create calendar for {}-{}", calendar.getYear(), calendar.getMonth());

        try {
            calendarService.validateCalendarData(calendar);
            DailyCalendar result = calendarService.createCalendar(calendar);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid calendar data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error creating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{year}/{month}")
    public ResponseEntity<DailyCalendar> updateCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @Valid @RequestBody DailyCalendar calendar) {
        log.info("REST request to update calendar for {}-{}", year, month);

        try {
            calendarService.validateCalendarData(calendar);
            DailyCalendar result = calendarService.updateCalendar(year, month, calendar);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid calendar data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<Void> deleteCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to delete calendar for {}-{}", year, month);

        try {
            calendarService.deleteCalendar(year, month);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== CALENDAR GENERATION =====

    @PostMapping("/generate/{year}/{month}")
    public ResponseEntity<DailyCalendar> generateCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @RequestBody(required = false) List<Integer> holidays) {
        log.info("REST request to generate calendar for {}-{} with holidays: {}", year, month, holidays);

        try {
            DailyCalendar result = calendarService.generateCalendar(year, month, holidays);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Error generating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== DAY MANAGEMENT =====

    @PutMapping("/{year}/{month}/day/{day}/type")
    public ResponseEntity<DailyCalendar> updateDayType(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @PathVariable @Min(1) @Max(31) int day,
            @RequestParam @Pattern(regexp = "^(working|holiday|weekend)$", message = "Type must be working, holiday, or weekend") String type) {
        log.info("REST request to update day type for {}-{}-{} to {}", year, month, day, type);

        try {
            DailyCalendar result = calendarService.updateDayType(year, month, day, type);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error updating day type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{year}/{month}/day")
    public ResponseEntity<DailyCalendar> addDayEntry(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @Valid @RequestBody DailyCalendar.DayEntry dayEntry) {
        log.info("REST request to add day entry for {}-{}-{}", year, month, dayEntry.getDay());

        try {
            DailyCalendar result = calendarService.addDayEntry(year, month, dayEntry);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error adding day entry: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ===== ATTENDANCE MANAGEMENT =====

    @PutMapping("/{year}/{month}/day/{day}/attendance")
    public ResponseEntity<DailyCalendar> updateAttendance(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @PathVariable @Min(1) @Max(31) int day,
            @RequestParam @Pattern(regexp = "^(wfoffice|wfh)$", message = "Attendance must be wfoffice or wfh") String attendance) {
        log.info("REST request to update attendance for {}-{}-{} to {}", year, month, day, attendance);

        try {
            DailyCalendar result = calendarService.updateAttendance(year, month, day, attendance);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid attendance type: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error updating attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{year}/{month}/day/{day}/attendance")
    public ResponseEntity<DailyCalendar> clearAttendance(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @PathVariable @Min(1) @Max(31) int day) {
        log.info("REST request to clear attendance for {}-{}-{}", year, month, day);

        try {
            // Pass null to clear attendance
            DailyCalendar result = calendarService.updateAttendance(year, month, day, null);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error clearing attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{year}/{month}/bulk-attendance")
    public ResponseEntity<List<DailyCalendar>> bulkUpdateAttendance(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @RequestBody Map<Integer, String> attendanceMap) {
        log.info("REST request to bulk update attendance for {}-{}", year, month);

        try {
            List<DailyCalendar> result = calendarService.bulkUpdateAttendance(year, month, attendanceMap);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error updating attendance in bulk: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{year}/{month}/bulk-day-types")
    public ResponseEntity<List<DailyCalendar>> bulkUpdateDayTypes(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @RequestBody Map<Integer, String> dayTypeMap) {
        log.info("REST request to bulk update day types for {}-{}", year, month);

        try {
            List<DailyCalendar> result = calendarService.bulkUpdateDayTypes(year, month, dayTypeMap);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error updating day types in bulk: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ===== STATISTICS =====

    @GetMapping("/{year}/{month}/stats")
    public ResponseEntity<Map<String, Object>> getCalendarStatistics(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to get statistics for {}-{}", year, month);

        try {
            Map<String, Object> stats = calendarService.getCalendarStatistics(year, month);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            log.error("Error getting statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/stats/overall")
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        log.info("REST request to get overall statistics");
        Map<String, Object> stats = calendarService.getOverallStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/year/{year}")
    public ResponseEntity<Map<String, Object>> getYearlyStatistics(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get yearly statistics for {}", year);
        Map<String, Object> stats = calendarService.getYearlyStatistics(year);
        return ResponseEntity.ok(stats);
    }

    // ===== QUERY ENDPOINTS =====

    @GetMapping("/year/{year}")
    public ResponseEntity<List<DailyCalendar>> getCalendarsByYear(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get calendars for year {}", year);
        return ResponseEntity.ok(calendarService.getCalendarsByYear(year));
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithHolidays() {
        log.info("REST request to get calendars with holidays");
        return ResponseEntity.ok(calendarService.getCalendarsWithHolidays());
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithAttendance() {
        log.info("REST request to get calendars with attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithAttendance());
    }

    @GetMapping("/attendance/office")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithOfficeAttendance() {
        log.info("REST request to get calendars with office attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithOfficeAttendance());
    }

    @GetMapping("/attendance/wfh")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithWfhAttendance() {
        log.info("REST request to get calendars with WFH attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithWfhAttendance());
    }

    @GetMapping("/attendance/mixed")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithMixedAttendance() {
        log.info("REST request to get calendars with mixed attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithMixedAttendance());
    }

    @GetMapping("/attendance/incomplete")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithIncompleteAttendance() {
        log.info("REST request to get calendars with incomplete attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithIncompleteAttendance());
    }

    @GetMapping("/attendance/full")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithFullAttendance() {
        log.info("REST request to get calendars with full attendance");
        return ResponseEntity.ok(calendarService.getCalendarsWithFullAttendance());
    }

    @GetMapping("/range")
    public ResponseEntity<List<DailyCalendar>> getCalendarsByDateRange(
            @RequestParam @Pattern(regexp = "\\d{4}", message = "Start year must be 4 digits") String startYear,
            @RequestParam @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Start month must be 01-12") String startMonth,
            @RequestParam @Pattern(regexp = "\\d{4}", message = "End year must be 4 digits") String endYear,
            @RequestParam @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "End month must be 01-12") String endMonth) {
        log.info("REST request to get calendars from {}-{} to {}-{}", startYear, startMonth, endYear, endMonth);
        return ResponseEntity.ok(calendarService.getCalendarsByDateRange(startYear, startMonth, endYear, endMonth));
    }

    // ===== UTILITY ENDPOINTS =====

    @GetMapping("/exists/{year}/{month}")
    public ResponseEntity<Boolean> calendarExists(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to check if calendar exists for {}-{}", year, month);
        boolean exists = calendarService.calendarExists(year, month);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCalendarsCount() {
        log.info("REST request to get total calendars count");
        return ResponseEntity.ok(calendarService.getTotalCalendarsCount());
    }

    @GetMapping("/years")
    public ResponseEntity<List<String>> getDistinctYears() {
        log.info("REST request to get distinct years");
        return ResponseEntity.ok(calendarService.getDistinctYears());
    }

    @GetMapping("/year/{year}/count")
    public ResponseEntity<Long> getCalendarCountByYear(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get calendar count for year {}", year);
        return ResponseEntity.ok(calendarService.getCalendarCountByYear(year));
    }
}