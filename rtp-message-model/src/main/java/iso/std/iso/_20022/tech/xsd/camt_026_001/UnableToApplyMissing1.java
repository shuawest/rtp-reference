//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.11.07 at 12:45:54 PM EST 
//


package iso.std.iso._20022.tech.xsd.camt_026_001;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UnableToApplyMissing1 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UnableToApplyMissing1">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Cd" type="{urn:iso:std:iso:20022:tech:xsd:camt.026.001.05}UnableToApplyMissingInformation3Code"/>
 *         &lt;element name="AddtlMssngInf" type="{urn:iso:std:iso:20022:tech:xsd:camt.026.001.05}Max140Text" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnableToApplyMissing1", propOrder = {
    "cd",
    "addtlMssngInf"
})
public class UnableToApplyMissing1 {

    @XmlElement(name = "Cd", required = true)
    @XmlSchemaType(name = "string")
    protected UnableToApplyMissingInformation3Code cd;
    @XmlElement(name = "AddtlMssngInf")
    protected String addtlMssngInf;

    /**
     * Gets the value of the cd property.
     * 
     * @return
     *     possible object is
     *     {@link UnableToApplyMissingInformation3Code }
     *     
     */
    public UnableToApplyMissingInformation3Code getCd() {
        return cd;
    }

    /**
     * Sets the value of the cd property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnableToApplyMissingInformation3Code }
     *     
     */
    public void setCd(UnableToApplyMissingInformation3Code value) {
        this.cd = value;
    }

    /**
     * Gets the value of the addtlMssngInf property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddtlMssngInf() {
        return addtlMssngInf;
    }

    /**
     * Sets the value of the addtlMssngInf property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddtlMssngInf(String value) {
        this.addtlMssngInf = value;
    }

}
