<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/*">
        <xsl:copy>
            <xsl:copy-of select="@*, //namespace::*, child::node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
