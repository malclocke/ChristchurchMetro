#!/usr/bin/env ruby

require 'rubygems'
require 'hpricot'

DATA_DIR="data_files/"
OUT_DIR="xml_platform_files/platforms"

xml = File.read(DATA_DIR + "JPPlatform.xml")
doc, platforms = Hpricot::XML(xml), []

(doc/:Platform).each do |platform|
  platform_number = platform.attributes['PlatformNo']
  platform_tag = platform.attributes['PlatformTag']

  if platform_number && platform_number.length > 0
    f = File.open("#{OUT_DIR}/numbers/#{platform_number}.xml", "w")
    f.puts '<?xml version="1.0" encoding="utf-8"?>'
    f.puts '<JPPlatforms xmlns="urn:connexionz-co-nz:jp" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:connexionz-co-nz:jp JourneyPlanner.xsd">'
    f.puts platform
    f.puts '</JPPlatforms>'
  end

  if platform_tag && platform_tag.length > 0
    f = File.open("#{OUT_DIR}/tags/#{platform_tag}.xml", "w")
    f.puts '<?xml version="1.0" encoding="utf-8"?>'
    f.puts '<JPPlatforms xmlns="urn:connexionz-co-nz:jp" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:connexionz-co-nz:jp JourneyPlanner.xsd">'
    f.puts platform
    f.puts '</JPPlatforms>'
  end
end
