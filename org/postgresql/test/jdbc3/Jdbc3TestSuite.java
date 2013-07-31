/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
*
*-------------------------------------------------------------------------
*/
package org.postgresql.test.jdbc3;

import org.postgresql.test.TestUtil;

import junit.framework.TestSuite;

/*
 * Executes all known tests for JDBC3
 */
public class Jdbc3TestSuite extends TestSuite
{

    /*
     * The main entry point for JUnit
     */
    public static TestSuite suite() throws Exception
    {
        Class.forName("org.postgresql.Driver");
        TestSuite suite = new TestSuite();
        try
        {
            java.sql.Connection con = TestUtil.openDB();
        
            if ( TestUtil.haveMinimumServerVersion( con, "8.1") && TestUtil.isProtocolVersion(con, 3))
            {
                suite.addTestSuite(Jdbc3CallableStatementTest.class);
            }
            if ( TestUtil.haveMinimumServerVersion( con, "8.2") )
            {
                suite.addTestSuite(GeneratedKeysTest.class);
            }
            con.close();
        }
        catch (Exception ex )
        {
            ex.printStackTrace();
        }
        
        // BEGIN_PGXC
        // PGXC does not support savepoints, so skip tests for savepoints
        if (!TestUtil.isPGXC())
        {
        // END_PGXC
        suite.addTestSuite(Jdbc3SavepointTest.class);
        // BEGIN_PGXC
        }
        // END_PGXC

        suite.addTestSuite(TypesTest.class);
        suite.addTestSuite(ResultSetTest.class);
        suite.addTestSuite(ParameterMetaDataTest.class);

        // BEGIN_PGXC
        // PGXC does not support large objects, so skip tests
        if (!TestUtil.isPGXC())
        {
        // END_PGXC
        suite.addTestSuite(Jdbc3BlobTest.class);
        // BEGIN_PGXC
        }
        // END_PGXC

        suite.addTestSuite(DatabaseMetaDataTest.class);
		suite.addTestSuite(SendRecvBufferSizeTest.class);
        suite.addTestSuite(StringTypeParameterTest.class);
        return suite;
    }
}
