package thredds.tds.ethan;

import junit.framework.TestCase;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsCrawl extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String catalog = "catalog.xml";

  private String catalogUrl;

  public TestTdsCrawl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    host = System.getProperty( "thredds.tds.test.server", host );
    catalog = System.getProperty( "thredds.tds.test.catalog", catalog );

    //targetTdsUrl = "http://" + host + "/thredds/";
    catalogUrl = "http://" + host + "/thredds/" + catalog;
  }

  public void testCrawlCatalog()
  {
    StringBuffer msg = new StringBuffer();

    assertTrue( "Invalid catalog(s) under catalog <" + catalogUrl + ">: " + msg.toString(),
                TestAll.openAndValidateCatalogTree( catalogUrl, msg, true ) );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

  public void testCrawlCatalogOpenOneDatasetInEachCollection()
  {

    StringBuffer msg = new StringBuffer();

    assertTrue( "Failed to open dataset(s) under catalog <" + catalogUrl + ">: " + msg.toString(),
                TestAll.crawlCatalogOpenRandomDataset( catalogUrl, msg ) );

    if ( msg.length() > 0 )
    {
      System.out.println( msg.toString() );
    }
  }

}
