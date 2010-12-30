#!/usr/bin/env ruby

require 'rubygems'
require 'hpricot'

DATA_DIR="data_files/"


xml = File.read(DATA_DIR + "JPRoutePattern.xml")
doc = Hpricot::XML(xml)

def escape(string)
  string.gsub /'/, "''"
end

lines = []

(doc/:Route).each do |route|
  #puts "INSERT INTO routes (route_number, name) VALUES ('#{escape route.attributes['RouteNo']}','#{escape route.attributes['Name']}');"
  (route/:Destination).each do |destination|
    #puts "INSERT INTO destinations (name) VALUES ('#{destination.attributes['Name']}');"
    (destination/:Pattern).each do |pattern|
      lines << "INSERT INTO patterns (route_number, route_name, destination, route_tag, pattern_name, direction, length, active) VALUES ('#{escape route.attributes['RouteNo']}', '#{escape route.attributes['Name']}', '#{escape destination.attributes['Name']}', '#{escape pattern.attributes['RouteTag']}','#{escape pattern.attributes['Name']}','#{escape pattern.attributes['Direction']}',#{escape pattern.attributes['Length']},'#{pattern.attributes['Schedule'] == "Active" ? 't' : 'f' }')"
      (destination/:Platform).each do |platform|
        lines << "INSERT INTO patterns_platforms (route_tag, platform_tag, schedule_adherance_timepoint) VALUES ('#{pattern.attributes['RouteTag']}', '#{platform.attributes['PlatformTag']}', '#{platform.attributes['ScheduleAdheranceTimepoint'] == 'true' ? 't' : 'f'}')"
      end
    end
  end
end

puts lines.join(";\n")
