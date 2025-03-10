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


package org.pentaho.big.data.impl.vfs.hdfs;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.hadoop.shim.api.hdfs.HadoopFileSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by bryan on 8/7/15.
 */
public class HDFSFileSystemTest {
  private FileName rootName;
  private HadoopFileSystem hadoopFileSystem;
  private HDFSFileSystem hdfsFileSystem;

  @Before
  public void setup() {
    rootName = mock( FileName.class );
    hadoopFileSystem = mock( HadoopFileSystem.class );
    hdfsFileSystem = new HDFSFileSystem( rootName, null, hadoopFileSystem );
  }

  @Test
  public void testAddCapabilities() {
    Collection caps = mock( Collection.class );
    hdfsFileSystem.addCapabilities( caps );
    verify( caps ).addAll( HDFSFileProvider.capabilities );
  }

  @Test
  public void testAddAppendCapabilities() {
    Collection caps = new ArrayList(  );
    when( hadoopFileSystem.getProperty( eq( "dfs.support.append" ), anyString() ) ).thenReturn( "false" );
    hdfsFileSystem.addCapabilities( caps );
    Collection res = new ArrayList( HDFSFileProvider.capabilities );
    assertArrayEquals( caps.toArray(), Collections.unmodifiableCollection( res ).toArray() );
    caps = new ArrayList(  );
    when( hadoopFileSystem.getProperty( eq( "dfs.support.append" ), anyString() ) ).thenReturn( "true" );
    hdfsFileSystem.addCapabilities( caps );
    res.add( Capability.APPEND_CONTENT );
    assertArrayEquals( caps.toArray(), Collections.unmodifiableCollection( res ).toArray() );
  }

  @Test
  public void testCreateFile() throws Exception {
    assertTrue( hdfsFileSystem.createFile( mock( AbstractFileName.class ) ) instanceof HDFSFileObject );
  }

  @Test
  public void testGetHDFSFileSystem() throws FileSystemException {
    assertEquals( hadoopFileSystem, hdfsFileSystem.getHDFSFileSystem() );
  }
}
