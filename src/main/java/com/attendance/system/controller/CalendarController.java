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
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @PathVariable @Min(1) @Max(31) int day) {

        try {
            Map<String, String> status = calendarService.getDayStatus(year, month, day);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for day status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting day status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<DailyCalendar>> getAllCalendars() {
        log.info("REST request to get all calendars");
        try {
            List<DailyCalendar> calendars = calendarService.getAllCalendars();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting all calendars: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<DailyCalendar> getCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to get calendar for {}-{}", year, month);

        try {
            Optional<DailyCalendar> calendar = calendarService.getCalendar(year, month);
            return calendar.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{year}/{month}/or-create")
    public ResponseEntity<DailyCalendar> getOrCreateCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @RequestParam(required = false) String regionCode) {
        log.info("REST request to get or create calendar for {}-{} with region {}", year, month, regionCode);

        try {
            DailyCalendar calendar;
            if (regionCode != null && !regionCode.trim().isEmpty()) {
                calendar = calendarService.getOrCreateCalendar(year, month, regionCode);
            } else {
                calendar = calendarService.getOrCreateCalendar(year, month);
            }
            return ResponseEntity.ok(calendar);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting or creating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<DailyCalendar> createCalendar(@Valid @RequestBody DailyCalendar calendar) {
        log.info("REST request to create calendar for {}-{}", calendar.getYear(), calendar.getMonth());

        try {
            DailyCalendar result = calendarService.createCalendar(calendar);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid calendar data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error creating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Unexpected error creating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{year}/{month}")
    public ResponseEntity<DailyCalendar> updateCalendar(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @Valid @RequestBody DailyCalendar calendar) {
        log.info("REST request to update calendar for {}-{}", year, month);

        try {
            DailyCalendar result = calendarService.updateCalendar(year, month, calendar);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid calendar data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating calendar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (IllegalArgumentException e) {
            log.error("Calendar not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
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
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for calendar generation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
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
            @RequestParam @Pattern(regexp = "^(working|holiday|weekend|leave)$",
                    message = "Type must be working, holiday, weekend, or leave") String type) {
        log.info("REST request to update day type for {}-{}-{} to {}", year, month, day, type);

        try {
            DailyCalendar result = calendarService.updateDayType(year, month, day, type);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request to update day type: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error updating day type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error updating day type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (IllegalArgumentException e) {
            log.error("Invalid day entry: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error adding day entry: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error adding day entry: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{year}/{month}/day/{day}/status")
    public ResponseEntity<DailyCalendar> updateDayStatus(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month,
            @PathVariable @Min(1) @Max(31) int day,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String attendance,
            @RequestParam(required = false) String description) {
        log.info("REST request to update day status for {}-{}-{}", year, month, day);

        try {
            DailyCalendar result = calendarService.updateDayStatus(year, month, day, type, attendance, description);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request to update day status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating day status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (Exception e) {
            log.error("Unexpected error updating attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (Exception e) {
            log.error("Unexpected error clearing attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (IllegalArgumentException e) {
            log.error("Invalid bulk attendance data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error updating attendance in bulk: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error in bulk attendance update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (IllegalArgumentException e) {
            log.error("Invalid bulk day type data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error updating day types in bulk: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Unexpected error in bulk day type update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        } catch (Exception e) {
            log.error("Unexpected error getting statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/overall")
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        log.info("REST request to get overall statistics");

        try {
            Map<String, Object> stats = calendarService.getOverallStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting overall statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/year/{year}")
    public ResponseEntity<Map<String, Object>> getYearlyStatistics(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get yearly statistics for {}", year);

        try {
            Map<String, Object> stats = calendarService.getYearlyStatistics(year);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting yearly statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== QUERY ENDPOINTS =====

    @GetMapping("/year/{year}")
    public ResponseEntity<List<DailyCalendar>> getCalendarsByYear(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get calendars for year {}", year);

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsByYear(year);
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithHolidays() {
        log.info("REST request to get calendars with holidays");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithHolidays();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with holidays: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithAttendance() {
        log.info("REST request to get calendars with attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance/office")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithOfficeAttendance() {
        log.info("REST request to get calendars with office attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithOfficeAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with office attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance/wfh")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithWfhAttendance() {
        log.info("REST request to get calendars with WFH attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithWfhAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with WFH attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance/mixed")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithMixedAttendance() {
        log.info("REST request to get calendars with mixed attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithMixedAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with mixed attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance/incomplete")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithIncompleteAttendance() {
        log.info("REST request to get calendars with incomplete attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithIncompleteAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with incomplete attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attendance/full")
    public ResponseEntity<List<DailyCalendar>> getCalendarsWithFullAttendance() {
        log.info("REST request to get calendars with full attendance");

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsWithFullAttendance();
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars with full attendance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/range")
    public ResponseEntity<List<DailyCalendar>> getCalendarsByDateRange(
            @RequestParam @Pattern(regexp = "\\d{4}", message = "Start year must be 4 digits") String startYear,
            @RequestParam @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Start month must be 01-12") String startMonth,
            @RequestParam @Pattern(regexp = "\\d{4}", message = "End year must be 4 digits") String endYear,
            @RequestParam @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "End month must be 01-12") String endMonth) {
        log.info("REST request to get calendars from {}-{} to {}-{}", startYear, startMonth, endYear, endMonth);

        try {
            List<DailyCalendar> calendars = calendarService.getCalendarsByDateRange(startYear, startMonth, endYear, endMonth);
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting calendars by date range: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== UTILITY ENDPOINTS =====

    @GetMapping("/exists/{year}/{month}")
    public ResponseEntity<Boolean> calendarExists(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year,
            @PathVariable @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12") String month) {
        log.info("REST request to check if calendar exists for {}-{}", year, month);

        try {
            boolean exists = calendarService.calendarExists(year, month);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Error checking calendar existence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCalendarsCount() {
        log.info("REST request to get total calendars count");

        try {
            Long count = calendarService.getTotalCalendarsCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting total calendars count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/years")
    public ResponseEntity<List<String>> getDistinctYears() {
        log.info("REST request to get distinct years");

        try {
            List<String> years = calendarService.getDistinctYears();
            return ResponseEntity.ok(years);
        } catch (Exception e) {
            log.error("Error getting distinct years: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/year/{year}/count")
    public ResponseEntity<Long> getCalendarCountByYear(
            @PathVariable @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        log.info("REST request to get calendar count for year {}", year);

        try {
            Long count = calendarService.getCalendarCountByYear(year);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting calendar count by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}