-- Create syntax for TABLE 'Blobdata'
CREATE TABLE `Blobdata` (
  `blobkey` varchar(255) NOT NULL DEFAULT '',
  `relativeFilename` varchar(255) NOT NULL DEFAULT '',
  `offset` int(11) NOT NULL,
  `size` int(11) NOT NULL,
  `lastUpdate` bigint(15) NOT NULL,
  `archived` tinyint(1) NOT NULL,
  `mimetype` varchar(255) DEFAULT '',
  PRIMARY KEY (`blobkey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'FileRecord'
CREATE TABLE `FileRecord` (
  `filename` varchar(255) NOT NULL DEFAULT '',
  `size` int(11) NOT NULL,
  PRIMARY KEY (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'RemovalfileRecord'
CREATE TABLE `RemovalfileRecord` (
  `filename` varchar(255) NOT NULL DEFAULT '',
  `size` int(11) NOT NULL,
  PRIMARY KEY (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



