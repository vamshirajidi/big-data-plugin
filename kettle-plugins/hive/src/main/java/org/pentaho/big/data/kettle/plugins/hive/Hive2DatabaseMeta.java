/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.big.data.kettle.plugins.hive;

import com.google.common.annotations.VisibleForTesting;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.plugins.DatabaseMetaPlugin;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.service.PluginServiceLoader;
import org.pentaho.hadoop.shim.api.cluster.NamedClusterService;
import org.pentaho.hadoop.shim.api.jdbc.DriverLocator;
import org.pentaho.metastore.api.exceptions.MetaStoreException;
import org.pentaho.metastore.locator.api.MetastoreLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@DatabaseMetaPlugin( type = "HIVE2", typeDescription = "Hadoop Hive 2/3" )
public class Hive2DatabaseMeta extends DatabaseMetaWithVersion {
  public static final String URL_PREFIX = "jdbc:hive2://";
  public static final String SELECT_COUNT_1_FROM = "select count(1) from ";
  public static final String ALIAS_SUFFIX = "_col";
  public static final String VIEW = "VIEW";
  public static final String VIRTUAL_VIEW = "VIRTUAL_VIEW";
  public static final String TRUNCATE_TABLE = "TRUNCATE TABLE ";
  public static final int[] ACCESS_TYPE_LIST = new int[] { DatabaseMeta.TYPE_ACCESS_NATIVE };
  protected static final String JAR_FILE = "hive-jdbc-0.10.0-pentaho.jar";
  protected static final String DRIVER_CLASS_NAME = "org.apache.hive.jdbc.HiveDriver";
  protected NamedClusterService namedClusterService;
  protected MetastoreLocator metastoreLocator;
  private Logger logger = LoggerFactory.getLogger( Hive2DatabaseMeta.class );

  //OSGi constructor
  public Hive2DatabaseMeta( DriverLocator driverLocator, NamedClusterService namedClusterService ) {
    super( driverLocator );
    this.namedClusterService = namedClusterService;
  }

  public synchronized MetastoreLocator getMetastoreLocator() {
    if ( this.metastoreLocator == null ) {
      try {
        Collection<MetastoreLocator> metastoreLocators = PluginServiceLoader.loadServices( MetastoreLocator.class );
        this.metastoreLocator = metastoreLocators.stream().findFirst().get();
      } catch ( Exception e ) {
        logger.error( "Error getting metastore locator", e );
      }
    }
    return this.metastoreLocator;
  }

  @VisibleForTesting
  protected Hive2DatabaseMeta( DriverLocator driverLocator, NamedClusterService namedClusterService,
                            MetastoreLocator metastoreLocator ) {
    super( driverLocator );
    this.namedClusterService = namedClusterService;
    this.metastoreLocator = metastoreLocator;
  }

  @Override
  public int[] getAccessTypeList() {
    return ACCESS_TYPE_LIST;
  }

  @Override
  public String getAddColumnStatement( String tablename, ValueMetaInterface v, String tk, boolean useAutoinc,
                                       String pk, boolean semicolon ) {

    return "ALTER TABLE " + tablename + " ADD " + getFieldDefinition( v, tk, pk, useAutoinc, true, false );

  }

  @Override
  public String getDriverClass() {

    //  !!!  We will probably have to change this if we are providing our own driver,
    //  i.e., before our code is committed to the Hadoop Hive project.
    return DRIVER_CLASS_NAME;
  }

  /**
   * This method assumes that Hive has no concept of primary and technical keys and auto increment columns. We are
   * ignoring the tk, pk and useAutoinc parameters.
   */
  @Override
  public String getFieldDefinition( ValueMetaInterface v, String tk, String pk, boolean useAutoinc,
                                    boolean addFieldname, boolean addCr ) {

    String retval = "";

    String fieldname = v.getName();
    int length = v.getLength();
    int precision = v.getPrecision();

    if ( addFieldname ) {
      retval += fieldname + " ";
    }

    int type = v.getType();
    switch ( type ) {

      case ValueMetaInterface.TYPE_BOOLEAN:
        retval += "BOOLEAN";
        break;

      case ValueMetaInterface.TYPE_DATE:
        retval += "DATE";
        break;

      case ValueMetaInterface.TYPE_TIMESTAMP:
        retval += "TIMESTAMP";
        break;

      case ValueMetaInterface.TYPE_STRING:
        retval += "STRING";
        break;

      case ValueMetaInterface.TYPE_NUMBER:
      case ValueMetaInterface.TYPE_INTEGER:
      case ValueMetaInterface.TYPE_BIGNUMBER:
        // Integer values...
        if ( precision == 0 ) {
          if ( length > 9 ) {
            if ( length < 19 ) {
              // can hold signed values between -9223372036854775808 and 9223372036854775807
              // 18 significant digits
              retval += "BIGINT";
            } else {
              retval += "FLOAT";
            }
          } else {
            retval += "INT";
          }
        } else {
          // Floating point values...
          if ( length > 15 ) {
            retval += "FLOAT";
          } else {
            // A double-precision floating-point number is accurate to approximately 15 decimal places.
            // http://mysql.mirrors-r-us.net/doc/refman/5.1/en/numeric-type-overview.html
            retval += "DOUBLE";
          }
        }

        break;
    }

    return retval;
  }

  @Override
  public String getModifyColumnStatement( String tablename, ValueMetaInterface v, String tk, boolean useAutoinc,
                                          String pk, boolean semicolon ) {

    return "ALTER TABLE " + tablename + " MODIFY " + getFieldDefinition( v, tk, pk, useAutoinc, true, false );
  }

  @Override
  public String getURL( String hostname, String port, String databaseName ) {

    return URL_PREFIX + hostname + ":" + port + "/" + databaseName;
  }

  @Override
  public String[] getUsedLibraries() {

    return new String[] { JAR_FILE };
  }

  /**
   * Build the SQL to count the number of rows in the passed table.
   *
   * @param tableName
   * @return
   */
  @Override
  public String getSelectCountStatement( String tableName ) {
    return SELECT_COUNT_1_FROM + tableName;
  }

  @Override
  public String generateColumnAlias( int columnIndex, String suggestedName ) {
    return suggestedName;
  }

  /**
   * Quotes around table names are not valid Hive QL
   * <p/>
   * return an empty string for the start quote
   */
  public String getStartQuote() {
    return "";
  }

  /**
   * Quotes around table names are not valid Hive QL
   * <p/>
   * return an empty string for the end quote
   */
  public String getEndQuote() {
    return "";
  }

  /**
   * @return a list of table types to retrieve tables for the database
   */
  @Override
  public String[] getTableTypes() {
    return null;
  }

  /**
   * @return a list of table types to retrieve views for the database
   */
  @Override
  public String[] getViewTypes() {
    return new String[] { VIEW, VIRTUAL_VIEW };
  }

  /**
   * @param tableName The table to be truncated.
   * @return The SQL statement to truncate a table: remove all rows from it without a transaction
   */
  @Override
  public String getTruncateTableStatement( String tableName ) {
    return TRUNCATE_TABLE + tableName;
  }

  @Override
  public boolean supportsSetCharacterStream() {
    return false;
  }

  @Override
  public boolean supportsBatchUpdates() {
    return false;
  }

  @Override
  public boolean supportsTimeStampToDateConversion() {
    return false;
  }

  @Override public List<String> getNamedClusterList() {
    try {
      return namedClusterService.listNames( getMetastoreLocator().getMetastore() );
    } catch ( MetaStoreException e ) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void putOptionalOptions( Map<String, String> extraOptions ) {
    if ( getNamedCluster() != null && getNamedCluster().trim().length() > 0 ) {
      extraOptions.put( getPluginId() + ".pentahoNamedCluster", getNamedCluster() );
    }
  }
}
