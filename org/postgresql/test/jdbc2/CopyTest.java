/*-------------------------------------------------------------------------
*
* Copyright (c) 2008-2011, PostgreSQL Global Development Group
*
*
*-------------------------------------------------------------------------
*/
package org.postgresql.test.jdbc2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.postgresql.PGConnection;
import org.postgresql.copy.*;
import org.postgresql.test.TestUtil;
import org.postgresql.util.PSQLState;

/**
 * @author kato@iki.fi
 *
 */
public class CopyTest extends TestCase {

    private Connection con;
    private CopyManager copyAPI;
    private String[] origData =
    { "First Row\t1\t1.10\n", // 0's required to match DB output for numeric(5,2)
      "Second Row\t2\t-22.20\n",
      "\\N\t\\N\t\\N\n",
      "\t4\t444.40\n" };
    private int dataRows = origData.length;

    public CopyTest(String name) {
        super(name);
    }

    private byte[] getData(String[] origData) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf);
        for(int i=0; i<origData.length; i++)
            ps.print(origData[i]);
        return buf.toByteArray();
    }

    protected void setUp() throws Exception {
        con = TestUtil.openDB();

        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            TestUtil.createTable(con, "copytest", "stringvalue text, intvalue int, numvalue numeric(5,2), i serial");
        }
        else
        {
        // END_PGXC
        TestUtil.createTable(con, "copytest", "stringvalue text, intvalue int, numvalue numeric(5,2)");
        // BEGIN_PGXC
        }
        // END_PGXC

        copyAPI = ((PGConnection)con).getCopyAPI();
    }

    protected void tearDown() throws Exception {
        TestUtil.dropTable(con, "copytest");
        TestUtil.closeDB(con);
    }

    private int getCount() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM copytest");
        rs.next();
        int result = rs.getInt(1);
        rs.close();
        return result;
    }
    
    public void testCopyInByRow() throws SQLException {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        CopyIn cp = copyAPI.copyIn(sql);
        for(int i=0; i<origData.length; i++) {
            byte[] buf = origData[i].getBytes();
            cp.writeToCopy(buf, 0, buf.length);
        }

        long count1 = cp.endCopy();
        long count2 = cp.getHandledRowCount();
        long expectedResult = -1;
        if (TestUtil.haveMinimumServerVersion(con, "8.2")) {
            expectedResult = dataRows;
        }
        assertEquals(expectedResult, count1);
        assertEquals(expectedResult, count2);

        try {
            cp.cancelCopy();
        } catch(SQLException se) { // should fail with obsolete operation
            if(! PSQLState.OBJECT_NOT_IN_STATE.getState().equals(se.getSQLState()) )
                 fail("should have thrown object not in state exception.");
        }
        int rowCount = getCount();
        assertEquals(dataRows, rowCount);
    }

    public void testCopyInAsOutputStream() throws SQLException, IOException {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        OutputStream os = new PGCopyOutputStream((PGConnection)con, sql, 1000);
        for(int i=0; i<origData.length; i++) {
            byte[] buf = origData[i].getBytes();
            os.write(buf);
        }
        os.close();
        int rowCount = getCount();
        assertEquals(dataRows, rowCount);
    }

    public void testCopyInFromInputStream() throws SQLException, IOException {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        copyAPI.copyIn(sql, new ByteArrayInputStream(getData(origData)), 3);
        int rowCount = getCount();
        assertEquals(dataRows, rowCount);
    }

    public void testCopyInFromStreamFail() throws SQLException {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        try {
            copyAPI.copyIn(sql, new InputStream() {
                public int read() { throw new RuntimeException("COPYTEST"); }
            }, 3 );
        } catch(Exception e) {
            if(e.toString().indexOf("COPYTEST") == -1)
                fail("should have failed trying to read from our bogus stream.");
        }
        int rowCount = getCount();
        assertEquals(0, rowCount);
    }

    public void testCopyInFromReader() throws SQLException, IOException {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        copyAPI.copyIn(sql, new StringReader(new String(getData(origData))), 3);
        int rowCount = getCount();
        assertEquals(dataRows, rowCount);
    }

    public void testSkipping() {
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest FROM STDIN";
        // BEGIN_PGXC
        }
        // END_PGXC

        String at = "init";
        int rowCount = -1;
        int skip = 0;
        int skipChar = 1;
        try {
            while(skipChar > 0) {
                at = "buffering";
                InputStream ins = new ByteArrayInputStream(getData(origData));
                at = "skipping";
                ins.skip(skip++);
                skipChar = ins.read();
                at = "copying";
                copyAPI.copyIn(sql, ins, 3);
                at = "using connection after writing copy";
                rowCount = getCount();
            }
        } catch(Exception e) {
            if( !( skipChar=='\t' ) ) // error expected when field separator consumed
                fail("testSkipping at " + at + " round " + skip + ": " + e.toString());
        }
        assertEquals(dataRows*(skip-1), rowCount);
    }
    
    public void testCopyOutByRow() throws SQLException, IOException {
        testCopyInByRow(); // ensure we have some data.

        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY (select stringvalue, intvalue, numvalue from copytest order by i) TO STDOUT";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest TO STDOUT";
        // BEGIN_PGXC
        }
        // END_PGXC

        CopyOut cp = copyAPI.copyOut(sql);
        int count = 0;
        byte buf[];
        while ( (buf = cp.readFromCopy()) != null) {
            count++;
        }
        assertEquals(false, cp.isActive());
        assertEquals(dataRows, count);

        long rowCount = cp.getHandledRowCount();
        long expectedResult = -1;
        if (TestUtil.haveMinimumServerVersion(con, "8.2")) {
            expectedResult = dataRows;
        }
        assertEquals(expectedResult, rowCount);

        assertEquals(dataRows, getCount());
    }

    public void testCopyOut() throws SQLException, IOException {
        testCopyInByRow(); // ensure we have some data.
        // XC needs to change the query, so declare the variable here
        String sql;
        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            sql = "COPY (select stringvalue, intvalue, numvalue from copytest order by i) TO STDOUT";
        }
        else
        {
        // END_PGXC
        sql = "COPY copytest TO STDOUT";
        // BEGIN_PGXC
        }
        // END_PGXC

        ByteArrayOutputStream copydata = new ByteArrayOutputStream();
        copyAPI.copyOut(sql, copydata);
        assertEquals(dataRows, getCount());
        // deep comparison of data written and read
        byte[] copybytes = copydata.toByteArray();
        assertTrue(copybytes != null);
        for(int i=0, l=0; i<origData.length; i++) {
            byte[] origBytes = origData[i].getBytes();
            assertTrue(origBytes != null);
            assertTrue("Copy is shorter than original", copybytes.length >= l + origBytes.length);
            for(int j=0; j<origBytes.length; j++, l++)
                assertEquals("content changed at byte#" + j + ": " +origBytes[j] +copybytes[l], origBytes[j], copybytes[l]);
        }
    }

    public void testNonCopyOut() throws SQLException, IOException {
        String sql = "SELECT 1";
        try {
            copyAPI.copyOut(sql, new ByteArrayOutputStream());
            fail("Can't use a non-copy query.");
        } catch (SQLException sqle) {
        }
        // Ensure connection still works.
        assertEquals(0, getCount());
    }

    public void testNonCopyIn() throws SQLException, IOException {
        String sql = "SELECT 1";
        try {
            copyAPI.copyIn(sql, new ByteArrayInputStream(new byte[0]));
            fail("Can't use a non-copy query.");
        } catch (SQLException sqle) {
        }
        // Ensure connection still works.
        assertEquals(0, getCount());
    }

    public void testStatementCopyIn() throws SQLException {
        Statement stmt = con.createStatement();

        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            try {
                stmt.execute("COPY copytest (stringvalue, intvalue, numvalue) FROM STDIN");
                fail("Should have failed because copy doesn't work from a Statement.");
            } catch (SQLException sqle) { }
        }
        else
        {
        // END_PGXC
        try {
            stmt.execute("COPY copytest FROM STDIN");
            fail("Should have failed because copy doesn't work from a Statement.");
        } catch (SQLException sqle) { }
        // BEGIN_PGXC
        }
        // END_PGXC
        stmt.close();

        assertEquals(0, getCount());
    }

    public void testStatementCopyOut() throws SQLException {
        testCopyInByRow(); // ensure we have some data.

        Statement stmt = con.createStatement();

        // BEGIN_PGXC
        if (TestUtil.isPGXC())
        {
            try {
                stmt.execute("COPY (select stringvalue, intvalue, numvalue from copytest order by i) TO STDOUT");
                fail("Should have failed because copy doesn't work from a Statement.");
            } catch (SQLException sqle) { }
        }
        else
        {
        // END_PGXC
        try {
            stmt.execute("COPY copytest TO STDOUT");
            fail("Should have failed because copy doesn't work from a Statement.");
        } catch (SQLException sqle) { }
        // BEGIN_PGXC
        }
        // END_PGXC
        stmt.close();

        assertEquals(dataRows, getCount());
    }

    public void testCopyQuery() throws SQLException, IOException {
        if (!TestUtil.haveMinimumServerVersion(con, "8.2"))
            return;

        testCopyInByRow(); // ensure we have some data.

        long count = copyAPI.copyOut("COPY (SELECT generate_series(1,1000)) TO STDOUT", new ByteArrayOutputStream());
        assertEquals(1000, count);
    }

    public void testCopyRollback() throws SQLException {
        con.setAutoCommit(false);
        testCopyInByRow();
        con.rollback();
        assertEquals(0, getCount());
    }

}
