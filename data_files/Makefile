# Run make in this directory to update all of the files

BASE=http://rtt.metroinfo.org.nz/RTT/Public/Utility/File.aspx?ContentType=SQLXML&Name=

.PHONY: all

all:
	for f in JPPlatformGroup.xml JPPlatform.xml JPRoutePattern.xml ScheduleMaster.xml ; do \
		wget -O $$f "$(BASE)$$f" ;\
	done
