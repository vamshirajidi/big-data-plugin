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


package org.pentaho.s3n.vfs;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.pentaho.s3common.S3CommonFileProvider;

public class S3NFileProvider extends S3CommonFileProvider {

  /**
   * The scheme this provider was designed to support
   */
  public static final String SCHEME = "s3n";

  /**
   * User Information.
   */
  public static final String ATTR_USER_INFO = "UI";

  public S3NFileProvider() {
    super();
    setFileNameParser( S3NFileNameParser.getInstance() );
  }

  protected FileSystem doCreateFileSystem( final FileName name, final FileSystemOptions fileSystemOptions )
    throws FileSystemException {
    return new S3NFileSystem( name, fileSystemOptions );
  }

}
