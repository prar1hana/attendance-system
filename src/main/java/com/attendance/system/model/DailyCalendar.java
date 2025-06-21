package com.attendance.system.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "daily_calendars")
@CompoundIndex(def = "{'year': 1, 'month': 1}", unique = true)
public class DailyCalendar {

    @Id
    private String id;

    @NotBlank(message = "Year is required")
    @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits")
    @Indexed
    private String year;

    @NotBlank(message = "Month is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Month must be 01-12")
    @Indexed
    private String month;

    @NotNull(message = "Days list is required")
    @Valid
    private List<DayEntry> days = new ArrayList<>();

    // Template tracking fields
    private String templateId; // Reference to base template if this is derived from one
    private String templateVersion = "1.0"; // Template versioning

    // Region/Organization support
    private String region = "default"; // For multi-region support
    private String organizationId; // For multi-tenant support

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public DailyCalendar(String year, String month) {
        this.year = year;
        this.month = month;
        this.days = new ArrayList<>();
    }

    public DailyCalendar(String year, String month, List<DayEntry> days) {
        this.year = year;
        this.month = month;
        this.days = days != null ? days : new ArrayList<>();
    }

    // Inner class for Day Entry
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayEntry {

        @Min(value = 1, message = "Day must be between 1 and 31")
        @Max(value = 31, message = "Day must be between 1 and 31")
        private int day; // 1 - 31

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "^(working|holiday|weekend|leave)$",
                message = "Type must be working, holiday, weekend, or leave")
        private String type; // "working", "holiday", "weekend", "leave"

        @Pattern(regexp = "^(wfoffice|wfh)$",
                message = "Attendance must be wfoffice or wfh")
        private String attendance; // "wfoffice", "wfh", or null

        // Track if this day has been manually updated from template
        private Boolean isUpdated = false;

        // Original template values for audit/reset purposes
        private String originalType;
        private String originalAttendance;

        // Additional metadata
        private LocalDateTime lastUpdated;
        private String updatedBy; // User ID who made the update
        private String description; // Additional notes

        public DayEntry(int day, String type, String attendance) {
            this.day = day;
            this.type = type;
            this.attendance = attendance;
            this.isUpdated = false;
            this.originalType = type;
            this.originalAttendance = attendance;
        }

        @Override
        public String toString() {
            return "DayEntry{" +
                    "day=" + day +
                    ", type='" + type + '\'' +
                    ", attendance='" + attendance + '\'' +
                    ", isUpdated=" + isUpdated +
                    '}';
        }
    }

    // ===== UTILITY METHODS =====

    public Optional<DayEntry> getDayEntry(int day) {
        return days.stream()
                .filter(d -> d.getDay() == day)
                .findFirst();
    }

    public void addDayEntry(DayEntry dayEntry) {
        if (dayEntry == null) {
            log.warn("Attempted to add null day entry");
            return;
        }

        // Remove existing entry for the same day if exists
        days.removeIf(d -> d.getDay() == dayEntry.getDay());
        days.add(dayEntry);
        log.debug("Added day entry for day {}", dayEntry.getDay());
    }

    public boolean updateAttendance(int day, String attendance) {
        Optional<DayEntry> dayEntryOpt = getDayEntry(day);
        if (dayEntryOpt.isEmpty()) {
            log.warn("Day {} not found in calendar {}-{}", day, year, month);
            return false;
        }

        DayEntry dayEntry = dayEntryOpt.get();
        if (!"working".equals(dayEntry.getType())) {
            log.warn("Cannot set attendance for non-working day {} in {}-{}", day, year, month);
            return false;
        }

        if (!java.util.Objects.equals(attendance, dayEntry.getAttendance())) {
            dayEntry.setAttendance(attendance);
            dayEntry.setIsUpdated(true);
            dayEntry.setLastUpdated(LocalDateTime.now());
            log.debug("Updated attendance for day {} in {}-{} to {}", day, year, month, attendance);
        }

        return true;
    }

    public boolean updateDayType(int day, String type) {
        Optional<DayEntry> dayEntryOpt = getDayEntry(day);
        if (dayEntryOpt.isEmpty()) {
            log.warn("Day {} not found in calendar {}-{}", day, year, month);
            return false;
        }

        DayEntry dayEntry = dayEntryOpt.get();
        if (!type.equals(dayEntry.getType())) {
            dayEntry.setType(type);
            dayEntry.setIsUpdated(true);
            dayEntry.setLastUpdated(LocalDateTime.now());

            // Clear attendance for non-working days
            if (!"working".equals(type)) {
                dayEntry.setAttendance(null);
            }

            log.debug("Updated day type for day {} in {}-{} to {}", day, year, month, type);
        }

        return true;
    }

    public boolean updateDayStatus(int day, String type, String attendance, String description) {
        Optional<DayEntry> dayEntryOpt = getDayEntry(day);
        if (dayEntryOpt.isEmpty()) {
            log.warn("Day {} not found in calendar {}-{}", day, year, month);
            return false;
        }

        DayEntry dayEntry = dayEntryOpt.get();

        // Update type
        if (type != null && !type.equals(dayEntry.getType())) {
            dayEntry.setType(type);
            dayEntry.setIsUpdated(true);
            dayEntry.setLastUpdated(LocalDateTime.now());
        }

        // Update attendance (only for working days)
        if ("working".equals(dayEntry.getType()) && attendance != null) {
            if (!attendance.equals(dayEntry.getAttendance())) {
                dayEntry.setAttendance(attendance);
                dayEntry.setIsUpdated(true);
                dayEntry.setLastUpdated(LocalDateTime.now());
            }
        } else if (!"working".equals(dayEntry.getType())) {
            // Clear attendance for non-working days
            dayEntry.setAttendance(null);
        }

        // Update description
        if (description != null) {
            dayEntry.setDescription(description);
        }

        return true;
    }

    // ===== STATISTICAL METHODS =====

    public long getWorkingDaysCount() {
        return days.stream()
                .filter(d -> "working".equals(d.getType()))
                .count();
    }

    public long getHolidaysCount() {
        return days.stream()
                .filter(d -> "holiday".equals(d.getType()))
                .count();
    }

    public long getWeekendsCount() {
        return days.stream()
                .filter(d -> "weekend".equals(d.getType()))
                .count();
    }

    public long getLeaveDaysCount() {
        return days.stream()
                .filter(d -> "leave".equals(d.getType()))
                .count();
    }

    public long getOfficeAttendanceCount() {
        return days.stream()
                .filter(d -> "wfoffice".equals(d.getAttendance()))
                .count();
    }

    public long getWfhAttendanceCount() {
        return days.stream()
                .filter(d -> "wfh".equals(d.getAttendance()))
                .count();
    }

    public long getAttendanceDaysCount() {
        return days.stream()
                .filter(d -> d.getAttendance() != null)
                .count();
    }

    public long getWorkingDaysWithoutAttendance() {
        return days.stream()
                .filter(d -> "working".equals(d.getType()) && d.getAttendance() == null)
                .count();
    }

    public long getUpdatedDaysCount() {
        return days.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsUpdated()))
                .count();
    }

    public double getAttendanceRate() {
        long workingDays = getWorkingDaysCount();
        if (workingDays == 0) return 0.0;
        return (double) getAttendanceDaysCount() / workingDays;
    }

    public double getOfficeAttendanceRate() {
        long attendanceDays = getAttendanceDaysCount();
        if (attendanceDays == 0) return 0.0;
        return (double) getOfficeAttendanceCount() / attendanceDays;
    }

    public double getWfhAttendanceRate() {
        long attendanceDays = getAttendanceDaysCount();
        if (attendanceDays == 0) return 0.0;
        return (double) getWfhAttendanceCount() / attendanceDays;
    }

    @Override
    public String toString() {
        return "DailyCalendar{" +
                "id='" + id + '\'' +
                ", year='" + year + '\'' +
                ", month='" + month + '\'' +
                ", templateId='" + templateId + '\'' +
                ", days=" + days.size() + " days" +
                '}';
    }
}