#!/usr/bin/env ruby

require 'rubygems'
require 'hpricot'

DATA_DIR="data_files/"

puts 'CREATE TABLE platforms (platform_tag int, platform_number int,
            name varchar ,road_name varchar, latitude double, longitude double);'

puts 'BEGIN TRANSACTION;'
xml = File.read(DATA_DIR + "JPPlatform.xml")
doc, platforms = Hpricot::XML(xml), []
(doc/:Platform).each do |platform|
  position = (platform/:Position).first
  puts "INSERT INTO platforms (platform_tag, platform_number, name, road_name, latitude, longitude)
                VALUES (%d,%s,\"%s\",\"%s\",%f,%f);" % [
                  platform.attributes['PlatformTag'],
                  platform.attributes['PlatformNo'].length > 0 ? platform.attributes['PlatformNo'] : 'NULL',
                  platform.attributes['Name'],
                  platform.attributes['RoadName'],
                  position.attributes['Lat'].to_f,
                  position.attributes['Long'].to_f,
                ]
end
puts 'COMMIT;'
