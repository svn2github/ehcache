<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" version="1.0" encoding="iso-8859-1" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="/">
      <xsl:for-each select="document/body/release">


ehcache-<xsl:value-of select="@version"/>
==================

        <xsl:copy-of select="."/> 
        

      </xsl:for-each>
</xsl:template>
</xsl:stylesheet>
