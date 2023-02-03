package com.bel.Zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.Base64.Decode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/*1. Read the entire XML.

2. Get signature from xml

3. Get Certificate from here.

4. If you have downloaded Offline XML before 7 June 2020. then get Certificate from here

5. If you have downloaded Offline XML before 18 Jun 2019. then get Certificate from here

6. If you have downloaded the client before 28 March, then get Certificate from here.

7. Convert certificate to base64 string.

8. Sample code snippets provided here.*/

public class DataFromZip {
	private static final String Mobile_Number = "9128704048";
	private static final String password = "1234";
	private static final Logger log = LoggerFactory.getLogger(DataFromZip.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DataFromZip dataFromZip = new DataFromZip();
		String signedXmlPath = dataFromZip.extractZipFile();
		log.info("SignedXml data is " + signedXmlPath);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(signedXmlPath));

			// Normalize the xml structure
			document.getDocumentElement().normalize();

//			System.out.println("document data is " + document);
//			 Get all the element by the tag name
//			NodeList Signature = document.getElementsByTagName("SignatureValue");
//			String signatureValue = document.getElementsByTagName("SignatureValue").item(0).getTextContent();
			String X509certificate = document.getElementsByTagName("X509Certificate").item(0).getTextContent();
			X509certificate = "-----BEGIN CERTIFICATE-----\n" + X509certificate + "\n-----END CERTIFICATE-----";
			log.info("  X509certificate is  : -> " + X509certificate);
			
			String certFile = dataFromZip.convertToCertificate(X509certificate);

			log.info(
//					"\u001B[34m" + 
			" certFile is :  " + certFile
//					+ "\u001B[34m" 
					);

			boolean signatureStatus = dataFromZip.verify(signedXmlPath, certFile);
			log.info(" xml verified status is :  " + signatureStatus);
			if (signatureStatus) {
				Boolean mobileNumber = dataFromZip.verifyMobileNumber(document);
				if (mobileNumber) {
					UserDetails userDetails = dataFromZip.getDataFromXMLFile(signedXmlPath);
					System.out.println(userDetails.toString());
				} else {
					System.out.println("Mobile Number Not matched, Unable to fetch userDetails");
				}

			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean verifyMobileNumber(Document document) {
		// TODO Auto-generated method stub
		NodeList OfflinePaperlessKyc = document.getElementsByTagName("OfflinePaperlessKyc");
		String ref_Id = OfflinePaperlessKyc.item(0).getAttributes().getNamedItem("referenceId").getNodeValue();
		NodeList nodeList = document.getElementsByTagName("Poi");
		String mob = nodeList.item(0).getAttributes().getNamedItem("m").getNodeValue();
		System.out.println(mob);
		System.out.println(nodeList.item(0).getAttributes().getNamedItem("name").getNodeValue());
		Boolean isMatched = isHashMatched(Mobile_Number, password, mob, ref_Id);
		if (isMatched) {
			System.out.println("Mobile number matched");
		} else
			System.out.println("Mobile number does not matched");
		return isMatched;

	}

	public UserDetails getDataFromXMLFile(String signedXmlPath) {
		// TODO Auto-generated method stub
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		UserDetails userDetails = new UserDetails();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			// Get Document
			Document document = builder.parse(new File(signedXmlPath));
			// Normalize the xml structure
			document.getDocumentElement().normalize();
			Element rootElement = document.getDocumentElement();
			NodeList nodeList = document.getElementsByTagName("Poi");
//            UserDetails userDetails = new UserDetails();
			userDetails.setDOB(nodeList.item(0).getAttributes().getNamedItem("dob").getNodeValue());
			userDetails.setGender(nodeList.item(0).getAttributes().getNamedItem("gender").getNodeValue());
			userDetails.setMobNumber(nodeList.item(0).getAttributes().getNamedItem("m").getNodeValue());
			userDetails.setName(nodeList.item(0).getAttributes().getNamedItem("name").getNodeValue());
//            System.out.println(nodeList.item(0).getAttributes().getNamedItem("dob").getNodeValue());
//            System.out.println(nodeList.item(0).getAttributes().getNamedItem("gender").getNodeValue());
//            System.out.println(nodeList.item(0).getAttributes().getNamedItem("m").getNodeValue());
//            System.out.println(nodeList.item(0).getAttributes().getNamedItem("name").getNodeValue());

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userDetails;
	}

	/**
	 * function to verify user's mobile number
	 **/
	private boolean isHashMatched(String mobileNo, String zipPassword, String hashedMobile, String ref_Id) {
		int aadharLastDigit = Character.getNumericValue(ref_Id.charAt(3));
		String concatedString = mobileNo + zipPassword;
		aadharLastDigit = aadharLastDigit == 0 ? 1 : aadharLastDigit; // if last
		// digit is "0", hash only one time.
		try {
			for (int i = 0; i < aadharLastDigit; i++) {
				concatedString = DigestUtils.sha256Hex(concatedString);
			}
			return hashedMobile.equals(concatedString);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean verify(String signedXml, String publicKeyFile) {

		boolean verificationResult = false;

		try {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(signedXml);
			NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (nl.getLength() == 0) {
				throw new IllegalArgumentException("Cannot find Signature element");
			}

			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

			DOMValidateContext valContext = new DOMValidateContext(getPublicKey(publicKeyFile), nl.item(0));
			valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
			XMLSignature signature = fac.unmarshalXMLSignature(valContext);
			verificationResult = signature.validate(valContext);

		} catch (Exception e) {
			System.out.println("Error while verifying digital siganature" + e.getMessage());
			e.printStackTrace();
		}

		return verificationResult;
	}

	private String convertToCertificate(String x509certificate) {
		// TODO Auto-generated method stub
		try {
//			FileWriter myWriter = new FileWriter("C:\\Users\\Dell\\Documents\\certificate.cer");
			FileWriter myWriter = new FileWriter("C:\\Users\\Dell\\Documents\\Bhanu\\certificate.cer");
			myWriter.write(x509certificate);
			myWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
//		return "C:\\Users\\Dell\\Documents\\certificate.cer";
		return "C:\\Users\\Dell\\Documents\\Bhanu\\certificate.cer";
	}

	private String extractZipFile() {
		// TODO Auto-generated method stub
//		ZipFile zipFile = new ZipFile("C:\\Users\\Dell\\Documents\\offlineaadhaar20221107060006746.zip",
//				password.toCharArray());
		
		ZipFile zipFile = new ZipFile("C:\\Users\\Dell\\Documents\\Bhanu\\offlineaadhaar20221107060006746.zip",
				password.toCharArray());
		
		try {
//			zipFile.extractFile("offlineaadhaar20221107060006746.xml", "C:\\Users\\Dell\\Documents");
			zipFile.extractFile("offlineaadhaar20221107060006746.xml", "C:\\Users\\Dell\\Documents\\Bhanu");
		} catch (ZipException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		return "C:\\Users\\Dell\\Documents\\offlineaadhaar20221107060006746.xml\\";
		return "C:\\Users\\Dell\\Documents\\Bhanu\\offlineaadhaar20221107060006746.xml\\";
	}

	private PublicKey getPublicKey(String certFile) throws Exception {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		FileInputStream fis = new FileInputStream(certFile);
		Certificate cert = factory.generateCertificate(fis);
		return cert.getPublicKey();
	}

}
