package com.attendance.system.repository;

import com.attendance.system.model.DailyCalendar;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCalendarRepository extends MongoRepository<DailyCalendar, String> {

    // ===== BASIC QUERIES =====

    // Find calendar by year and month
    Optional<DailyCalendar> findByYearAndMonth(String year, String month);

    // Find all calendars for a specific year
    List<DailyCalendar> findByYear(String year);

    // Find calendars by year range
    List<DailyCalendar> findByYearBetween(String startYear, String endYear);

    // Check if calendar exists for year and month
    boolean existsByYearAndMonth(String year, String month);

    // Delete calendar by year and month
    void deleteByYearAndMonth(String year, String month);

    // Find by region
    List<DailyCalendar> findByRegion(String region);

    // Find by organization
    List<DailyCalendar> findByOrganizationId(String organizationId);

    // Find by template ID
    List<DailyCalendar> findByTemplateId(String templateId);

    // ===== DAY TYPE QUERIES =====

    // Find calendars with holidays
    @Query("{'days.type': 'holiday'}")
    List<DailyCalendar> findCalendarsWithHolidays();

    // Find calendars with weekends
    @Query("{'days.type': 'weekend'}")
    List<DailyCalendar> findCalendarsWithWeekends();

    // Find calendars with working days
    @Query("{'days.type': 'working'}")
    List<DailyCalendar> findCalendarsWithWorkingDays();

    // Find calendars by specific day type
    @Query("{'days.type': ?0}")
    List<DailyCalendar> findByDayType(String dayType);

    // ===== ATTENDANCE QUERIES =====

    // Find calendars with any attendance data
    @Query("{'days.attendance': {$exists: true, $ne: null}}")
    List<DailyCalendar> findCalendarsWithAttendance();

    // Find calendars by specific attendance type
    @Query("{'days.attendance': ?0}")
    List<DailyCalendar> findByAttendanceType(String attendanceType);

    // Find calendars with office attendance
    @Query("{'days.attendance': 'wfoffice'}")
    List<DailyCalendar> findCalendarsWithOfficeAttendance();

    // Find calendars with work from home attendance
    @Query("{'days.attendance': 'wfh'}")
    List<DailyCalendar> findCalendarsWithWfhAttendance();

    // Find calendars with mixed attendance (both office and wfh)
    @Query("{'$and': [{'days.attendance': 'wfoffice'}, {'days.attendance': 'wfh'}]}")
    List<DailyCalendar> findCalendarsWithMixedAttendance();

    // ===== TEMPORAL QUERIES =====

    // Find latest calendar (most recent year-month)
    @Query(value = "{}", sort = "{'year': -1, 'month': -1}")
    Optional<DailyCalendar> findLatestCalendar();

    // Find oldest calendar (earliest year-month)
    @Query(value = "{}", sort = "{'year': 1, 'month': 1}")
    Optional<DailyCalendar> findOldestCalendar();

    // Find calendars in date range
    @Query("{'$and': [" +
            "{'$or': [{'year': {'$gt': ?0}}, {'$and': [{'year': ?0}, {'month': {'$gte': ?1}}]}]}," +
            "{'$or': [{'year': {'$lt': ?2}}, {'$and': [{'year': ?2}, {'month': {'$lte': ?3}}]}]}" +
            "]}")
    List<DailyCalendar> findByDateRange(String startYear, String startMonth, String endYear, String endMonth);

    // Find calendars for current year
    @Query("{'year': ?0}")
    List<DailyCalendar> findByCurrentYear(String currentYear);

    // ===== STATISTICAL QUERIES =====

    // Count working days across all calendars
    @Query(value = "{'days.type': 'working'}", count = true)
    long countWorkingDays();

    // Count holidays across all calendars
    @Query(value = "{'days.type': 'holiday'}", count = true)
    long countHolidays();

    // Count weekends across all calendars
    @Query(value = "{'days.type': 'weekend'}", count = true)
    long countWeekends();

    // Count office attendance days
    @Query(value = "{'days.attendance': 'wfoffice'}", count = true)
    long countOfficeAttendanceDays();

    // Count work from home days
    @Query(value = "{'days.attendance': 'wfh'}", count = true)
    long countWfhAttendanceDays();

    // ===== COMPLEX QUERIES =====

    // Find calendars with specific day and attendance combination
    @Query("{'days': {'$elemMatch': {'day': ?0, 'type': ?1, 'attendance': ?2}}}")
    List<DailyCalendar> findByDayTypeAndAttendance(int day, String type, String attendance);

    // Find calendars where specific day is a working day with attendance
    @Query("{'days': {'$elemMatch': {'day': ?0, 'type': 'working', 'attendance': {'$exists': true, '$ne': null}}}}")
    List<DailyCalendar> findByWorkingDayWithAttendance(int day);

    // Find calendars with attendance percentage greater than threshold
    @Aggregation(pipeline = {
            "{ '$addFields': { " +
                    "   'workingDaysCount': { '$size': { '$filter': { 'input': '$days', 'cond': { '$eq': ['$$this.type', 'working'] } } } }, " +
                    "   'attendanceDaysCount': { '$size': { '$filter': { 'input': '$days', 'cond': { '$and': [ { '$eq': ['$$this.type', 'working'] }, { '$ne': ['$$this.attendance', null] } ] } } } } " +
                    " } }",
            "{ '$addFields': { " +
                    "   'attendanceRate': { " +
                    "     '$cond': { " +
                    "       'if': { '$eq': ['$workingDaysCount', 0] }, " +
                    "       'then': 0, " +
                    "       'else': { '$divide': ['$attendanceDaysCount', '$workingDaysCount'] } " +
                    "     } " +
                    "   } " +
                    " } }",
            "{ '$match': { 'attendanceRate': { '$gte': ?0 } } }"
    })
    List<DailyCalendar> findByAttendanceRateGreaterThan(double threshold);

    // Find calendars by year with attendance statistics
    @Aggregation(pipeline = {
            "{ '$match': { 'year': ?0 } }",
            "{ '$addFields': { " +
                    "   'totalWorkingDays': { '$size': { '$filter': { 'input': '$days', 'cond': { '$eq': ['$$this.type', 'working'] } } } }, " +
                    "   'officeAttendance': { '$size': { '$filter': { 'input': '$days', 'cond': { '$eq': ['$$this.attendance', 'wfoffice'] } } } }, " +
                    "   'wfhAttendance': { '$size': { '$filter': { 'input': '$days', 'cond': { '$eq': ['$$this.attendance', 'wfh'] } } } } " +
                    " } }"
    })
    List<DailyCalendar> findByYearWithAttendanceStats(String year);

    // ===== UTILITY QUERIES =====

    // Find calendars with incomplete attendance (working days without attendance)
    @Query("{'days': {'$elemMatch': {'type': 'working', '$or': [{'attendance': null}, {'attendance': {'$exists': false}}]}}}")
    List<DailyCalendar> findCalendarsWithIncompleteAttendance();

    // Find calendars with full attendance (all working days have attendance)
    @Query("{'days': {'$not': {'$elemMatch': {'type': 'working', '$or': [{'attendance': null}, {'attendance': {'$exists': false}}]}}}}")
    List<DailyCalendar> findCalendarsWithFullAttendance();

    // Find calendars where all working days have attendance
    @Query("{'$and': [" +
            "{'days.type': 'working'}, " +
            "{'days': {'$not': {'$elemMatch': {'type': 'working', '$or': [{'attendance': null}, {'attendance': {'$exists': false}}]}}}}" +
            "]}")
    List<DailyCalendar> findCalendarsWithCompleteAttendance();

    // Count calendars by year
    long countByYear(String year);

    // Count calendars by region
    long countByRegion(String region);

    // Count calendars by organization
    long countByOrganizationId(String organizationId);

    // Find distinct years in the database
    @Query(value = "{}", fields = "{'year': 1}")
    List<String> findDistinctYears();

    // Find distinct regions
    @Query(value = "{}", fields = "{'region': 1}")
    List<String> findDistinctRegions();

    // Find distinct organizations
    @Query(value = "{}", fields = "{'organizationId': 1}")
    List<String> findDistinctOrganizations();

    // ===== TEMPLATE RELATED QUERIES =====

    // Find calendars derived from a specific template
    List<DailyCalendar> findByTemplateIdAndTemplateVersion(String templateId, String templateVersion);

    // Find calendars that have been updated from their original template
    @Query("{'days.isUpdated': true}")
    List<DailyCalendar> findCalendarsWithUpdatedDays();

    // Find calendars created in a specific time period
    @Query("{'createdAt': {'$gte': ?0, '$lte': ?1}}")
    List<DailyCalendar> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Find recently modified calendars
    @Query("{'updatedAt': {'$gte': ?0}}")
    List<DailyCalendar> findByUpdatedAtAfter(java.time.LocalDateTime since);
}