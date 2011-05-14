#!/usr/bin/env ruby

# Send the output of this to res/raw/platforms_sql

require 'rubygems'
require 'hpricot'

DATA_DIR="data_files/"

xml = File.read(DATA_DIR + "JPPlatform.xml")
lines = []
doc, platforms = Hpricot::XML(xml), []
(doc/:Platform).each do |platform|
  position = (platform/:Position).first
  lines << "INSERT INTO platforms (platform_tag, platform_number, name, road_name, latitude, longitude)
                VALUES (%d,%s,\"%s\",\"%s\",%f,%f)" % [
                  platform.attributes['PlatformTag'],
                  platform.attributes['PlatformNo'].length > 0 ? platform.attributes['PlatformNo'] : 'NULL',
                  platform.attributes['Name'],
                  platform.attributes['RoadName'],
                  position.attributes['Lat'].to_f,
                  position.attributes['Long'].to_f,
                ]
end
puts lines.join(";\n")
