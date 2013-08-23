#!/usr/bin/python
# -*- coding: utf-8 -*-

import urllib2
import filecmp
import os
import tempfile
import time
import random
import string
import sys


if len(sys.argv) < 2:
	print "no binary file provided."
	sys.exit(0)


class HttpHelper:
	uri = ''
	filename = ''
	tmpFilename = ''
	def __init__(self, uri, filename):
		self.uri = uri
		self.filename = filename
	def create(self):
		bytes_read = open(self.filename, "rb").read()
		req = urllib2.Request(self.uri, bytes_read, {'Content-Type': 'application/octet-stream'})
		urllib2.urlopen(req)
	def read(self):
		bytes_back = urllib2.urlopen(self.uri).read()
		self.tmpFilename = tempfile.mktemp()
		open(self.tmpFilename, "wb").write(bytes_back)
	def removeServer(self):
		opener = urllib2.build_opener(urllib2.HTTPHandler)
		request = urllib2.Request(self.uri)
		request.get_method = lambda: 'DELETE'
		url = opener.open(request)
	def removeLocal(self):
		os.remove(self.tmpFilename)

print "Using file " + sys.argv[1]


def simpleCreateReadDeleteTest():
	newId = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(10))
	httpHelp = HttpHelper('http://localhost:8080/' + newId, sys.argv[1])
	httpHelp.create()
	httpHelp.read()
	if filecmp.cmp(httpHelp.filename, httpHelp.tmpFilename) == False:
		print "Failed to validate binary file post/get"
	httpHelp.removeServer()
	httpHelp.removeLocal()

start_time = time.time()
i=0
while (i < 5000):
	simpleCreateReadDeleteTest()
	i += 1
	#time.sleep(1)
	
print time.time() - start_time, "seconds"