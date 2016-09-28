<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
				xmlns:geomark="http://gov.bc.ca/geomark"
				xmlns:gml="http://www.opengis.net/gml"
				exclude-result-prefixes="gml geomark">
	
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>
 	
 	<xsl:template match="/">
		<FEATURECOLLECTION>
				<xsl:apply-templates select="gml:FeatureCollection/gml:featureMember" />						
		</FEATURECOLLECTION>
	</xsl:template>
	<xsl:template match="gml:featureMember">
		<FEATURE>
      <xsl:for-each select="geomark:GeomarkPart/geomark:geometry/gml:Point">
        <xsl:call-template name="spatial-data"/>
      </xsl:for-each>
      <xsl:for-each select="geomark:GeomarkPart/geomark:geometry/gml:LineString">
        <xsl:call-template name="spatial-data"/>
      </xsl:for-each>
      <xsl:for-each select="geomark:GeomarkPart/geomark:geometry/gml:Polygon">
        <xsl:call-template name="spatial-data"/>
      </xsl:for-each>
    </FEATURE>    
	</xsl:template>
	<xsl:template name="spatial-data">
			<GEOMETRY>
	    		<xsl:call-template name="RemoveNS" />				      	
			</GEOMETRY>		
	</xsl:template>
	<xsl:template name="RemoveNS" match="*">
  		<xsl:element name="{local-name()}">
    		<xsl:apply-templates />
  		</xsl:element>
	</xsl:template>  		
</xsl:stylesheet>