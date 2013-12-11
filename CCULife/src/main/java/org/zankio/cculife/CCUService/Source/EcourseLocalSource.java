package org.zankio.cculife.CCUService.Source;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import org.zankio.cculife.CCUService.Ecourse;
import org.zankio.cculife.database.EcourseDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class EcourseLocalSource extends EcourseSource {

    private Ecourse ecourse;
    private EcourseDatabaseHelper databaseHelper;
    private SQLiteDatabase database;
    private Ecourse.Course currentCourse;

    private String[] ecourseColumns = {
            EcourseDatabaseHelper.LIST_COLUMN_COURSEID,
            EcourseDatabaseHelper.LIST_COLUMN_NAME,
            EcourseDatabaseHelper.LIST_COLUMN_TEACHER,
            EcourseDatabaseHelper.LIST_COLUMN_HOMEWORK,
            EcourseDatabaseHelper.LIST_COLUMN_NOTICE,
            EcourseDatabaseHelper.LIST_COLUMN_EXAM,
            EcourseDatabaseHelper.LIST_COLUMN_WARNING
    };

    private String[] announceColumns = {
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_TITLE,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_CONTENT,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_URL,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_DATE,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_BROWSECOUNT,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_IMPORTANT,
            EcourseDatabaseHelper.ANNOUNCE_COLUMN_NEW
    };

    private String[] scoreColumns = {
            EcourseDatabaseHelper.SCORE_COLUMN_NAME,
            EcourseDatabaseHelper.SCORE_COLUMN_PERCENT,
            EcourseDatabaseHelper.SCORE_COLUMN_RANK,
            EcourseDatabaseHelper.SCORE_COLUMN_SCORE,
            EcourseDatabaseHelper.SCORE_COLUMN_HEADER
    };

    public EcourseLocalSource(Ecourse ecourse, Context context) {
        this.databaseHelper = new EcourseDatabaseHelper(context);
        this.database = databaseHelper.getWritableDatabase();
        this.ecourse = ecourse;
    }

    @Override
    public void openSource() {
        this.database = databaseHelper.getWritableDatabase();
    }

    @Override
    public void closeSource() {
        databaseHelper.close();
    }

    @Override
    public void switchCourse(Ecourse.Course course) {
        currentCourse = course;
    }

    @Override
    public Ecourse.Course[] getCourse() throws Exception {
        List<Ecourse.Course> result = new ArrayList<Ecourse.Course>();

        Cursor cursor = database.query(
                EcourseDatabaseHelper.TABLE_ECOURSE,
                ecourseColumns,
                null, null, null, null, null
        );

        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            result.add(cursorToCourse(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return result.toArray(new Ecourse.Course[result.size()]);
    }

    private Ecourse.Course cursorToCourse(Cursor cursor) {
        Ecourse.Course course = ecourse.new Course(ecourse);
        course.setCourseid(cursor.getString(0));
        course.setName(cursor.getString(1));
        course.setTeacher(cursor.getString(2));
        course.setHomework(cursor.getInt(3));
        course.setNotice(cursor.getInt(4));
        course.setExam(cursor.getInt(5));
        course.setWarning(cursor.getInt(6) == 1);
        return course;
    }

    public Ecourse.Course[] storeCourse(Ecourse.Course[] courses) {

        database.delete(EcourseDatabaseHelper.TABLE_ECOURSE, null, null);

        ContentValues values = new ContentValues();
        for(Ecourse.Course course : courses) {
            values.clear();
            values.put(EcourseDatabaseHelper.LIST_COLUMN_COURSEID, course.getCourseid());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_NAME, course.getName());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_TEACHER, course.getTeacher());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_HOMEWORK, course.getHomework());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_NOTICE, course.getNotice());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_EXAM, course.getExam());
            values.put(EcourseDatabaseHelper.LIST_COLUMN_WARNING, course.isWarning() ? 1 : 0);
            database.insert(EcourseDatabaseHelper.TABLE_ECOURSE, null, values);
        }

        return courses;
    }

    @Override
    public Ecourse.Scores[] getScore(Ecourse.Course course) throws Exception {
        List<Ecourse.Scores> Scores = new ArrayList<Ecourse.Scores>();
        List<Ecourse.Score> Score;
        Cursor cursorHeader, cursorScore;
        Ecourse.Scores now;

        cursorHeader = database.query(
                EcourseDatabaseHelper.TABLE_ECOURSE_SCORE,
                scoreColumns,
                EcourseDatabaseHelper.SCORE_COLUMN_HEADER + "<0 AND " +
                        EcourseDatabaseHelper.SCORE_COLUMN_COURSEID + "=\"" + course.getCourseid() + "\"",
                null, null, null, null
        );

        cursorHeader.moveToFirst();
        while(!cursorHeader.isAfterLast()) {
            now = cursorToScores(cursorHeader);

            cursorScore = database.query(
                    EcourseDatabaseHelper.TABLE_ECOURSE_SCORE,
                    scoreColumns,
                    EcourseDatabaseHelper.SCORE_COLUMN_HEADER + "=" + (-cursorHeader.getInt(4)) + " AND " +
                            EcourseDatabaseHelper.SCORE_COLUMN_COURSEID + "=\"" + course.getCourseid() + "\"",
                    null, null, null, null
            );

            Score = new ArrayList<Ecourse.Score>();

            cursorScore.moveToFirst();
            while(!cursorScore.isAfterLast()) {
                Score.add(cursorToScore(cursorScore));
                cursorScore.moveToNext();
            }

            now.scores = Score.toArray(new Ecourse.Score[Score.size()]);

            Scores.add(now);
            cursorScore.close();
            cursorHeader.moveToNext();
        }
        cursorHeader.close();

        return Scores.toArray(new Ecourse.Scores[Scores.size()]);
    }

    public Ecourse.Scores[] storeScores(Ecourse.Scores[] scores, Ecourse.Course course) {
        int i = 1;

        database.delete(
                EcourseDatabaseHelper.TABLE_ECOURSE_SCORE,
                EcourseDatabaseHelper.SCORE_COLUMN_COURSEID + "=\"" + course.getCourseid() + "\"",
                null
        );

        ContentValues values = new ContentValues();
        for(Ecourse.Scores scoreHeader : scores) {
            values.clear();
            values.put(EcourseDatabaseHelper.SCORE_COLUMN_NAME, scoreHeader.Name);
            values.put(EcourseDatabaseHelper.SCORE_COLUMN_SCORE, scoreHeader.Score);
            values.put(EcourseDatabaseHelper.SCORE_COLUMN_RANK, scoreHeader.Rank);
            values.put(EcourseDatabaseHelper.SCORE_COLUMN_COURSEID, course.getCourseid());
            values.put(EcourseDatabaseHelper.SCORE_COLUMN_HEADER, -i);
            database.insert(EcourseDatabaseHelper.TABLE_ECOURSE_SCORE, null, values);

            if(scoreHeader.scores != null) {
                for(Ecourse.Score score: scoreHeader.scores) {
                    values.clear();
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_NAME, score.Name);
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_SCORE, score.Score);
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_RANK, score.Rank);
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_PERCENT, score.Rank);
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_COURSEID, course.getCourseid());
                    values.put(EcourseDatabaseHelper.SCORE_COLUMN_HEADER, i);
                    database.insert(EcourseDatabaseHelper.TABLE_ECOURSE_SCORE, null, values);
                }
            }
            i++;
        }

        return scores;
    }

    private Ecourse.Scores cursorToScores(Cursor cursor) {
        Ecourse.Scores scores = ecourse.new Scores();
        scores.Name = cursor.getString(0);
        scores.Rank = cursor.getString(2);
        scores.Score = cursor.getString(3);
        return scores;
    }

    private Ecourse.Score cursorToScore(Cursor cursor) {
        Ecourse.Score score = ecourse.new Score();
        score.Name = cursor.getString(0);
        score.Percent = cursor.getString(1);
        score.Rank = cursor.getString(2);
        score.Score = cursor.getString(3);
        return score;
    }

    @Override
    public Ecourse.Classmate[] getClassmate(Ecourse.Course course) throws Exception {
        throw new Exception("尚未支援離線資料");
        //return new Ecourse.Classmate[0];
    }

    /*private Ecourse.Classmate cursorToClassmate(Cursor cursor) {
        Ecourse.Classmate classmate = ecourse.new Classmate(ecourse);
        classmate.Name
        classmate.Gender
        classmate.StudentId
        classmate.Department
        return course;
    }*/

    public boolean hasAnnounceContent(Ecourse.Announce announce) {
        int result;

        Cursor cursor = database.query(
                EcourseDatabaseHelper.TABLE_ECOURSE_ANNOUNCE,
                new String[] {EcourseDatabaseHelper.ANNOUNCE_COLUMN_CONTENT},
                EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID + "=\"" + announce.getCourseID() + "\" AND " +
                        EcourseDatabaseHelper.ANNOUNCE_COLUMN_URL + " =\"" + DatabaseUtils.sqlEscapeString(announce.url) + "\"",
                null, null, null, null
        );

        result = cursor.getCount();

        cursor.close();
        return result > 0;
    }

    @Override
    public Ecourse.Announce[] getAnnounces(Ecourse.Course course) throws Exception {
        List<Ecourse.Announce> result = new ArrayList<Ecourse.Announce>();

        Cursor cursor = database.query(
                EcourseDatabaseHelper.TABLE_ECOURSE_ANNOUNCE,
                announceColumns,
                EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID + "=\"" + course.getCourseid() + "\"",
                null, null, null, null
        );

        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            result.add(cursorToAnnounce(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return result.toArray(new Ecourse.Announce[result.size()]);
    }

    private Ecourse.Announce cursorToAnnounce(Cursor cursor) {
        Ecourse.Course course = ecourse.new Course(ecourse);
        course.setCourseid(cursor.getString(0));

        Ecourse.Announce announce = ecourse.new Announce(ecourse, course);
        announce.Title = cursor.getString(1);
        announce.Content = cursor.getString(2);
        announce.url = cursor.getString(3);
        announce.Date = cursor.getString(4);
        announce.browseCount = cursor.getInt(5);
        announce.important = cursor.getString(6);
        announce.isnew = cursor.getInt(7) == 1;
        return announce;
    }

    public Ecourse.Announce[] storeAnnounce(Ecourse.Announce[] announces, Ecourse.Course course) {

        database.delete(
                EcourseDatabaseHelper.TABLE_ECOURSE_ANNOUNCE,
                EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID + "=\"" + course.getCourseid() + "\"",
                null
        );

        ContentValues values = new ContentValues();
        for(Ecourse.Announce announce : announces) {
            values.clear();
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_TITLE, announce.Title);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_BROWSECOUNT, announce.browseCount);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_CONTENT, announce.Content);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID, course.getCourseid());
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_DATE, announce.Date);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_IMPORTANT, announce.important);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_NEW, announce.isnew ? 1 : 0);
            values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_URL, announce.url);
            database.insert(EcourseDatabaseHelper.TABLE_ECOURSE_ANNOUNCE, null, values);
        }

        return announces;
    }

    @Override
    public String getAnnounceContent(Ecourse.Announce announce) throws Exception {
        throw new Exception("無離線資料");
    }

    public String storeAnnounceContent(String content, Ecourse.Announce announce) {
        ContentValues values = new ContentValues();
        values.put(EcourseDatabaseHelper.ANNOUNCE_COLUMN_CONTENT, content);

        database.update(
                EcourseDatabaseHelper.TABLE_ECOURSE_ANNOUNCE,
                values,
                EcourseDatabaseHelper.ANNOUNCE_COLUMN_COURSEID + "=\"" + announce.getCourseID() + "\" AND " +
                    EcourseDatabaseHelper.ANNOUNCE_COLUMN_URL + "=\"" + DatabaseUtils.sqlEscapeString(announce.url) + "\"",
                null
        );
        return content;
    }



    @Override
    public Ecourse.File[] getFiles(Ecourse.Course course) throws Exception {
        throw new Exception("尚未支援離線資料");
        //return new Ecourse.File[0];
    }

}
