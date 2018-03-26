package com.rallydev.integration.build.testbuild;

import junit.framework.TestCase;

public class TestBuildTest
    extends TestCase
{
    public void testMe(  )
    {
        String failme = System.getProperty( "failme" );
        System.out.println( "fail me:" + failme );

        if ( ( failme != null ) && failme.equals( "true" ) )
        {
            fail(  );
        }
    }
}
