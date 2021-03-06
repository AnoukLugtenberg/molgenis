package org.molgenis.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ISO 3166-2 country codes
 */
public class CountryCodes
{
	private static final Map<String, String> COUNTRIES;

	static
	{
		COUNTRIES = new LinkedHashMap<>();
		COUNTRIES.put("AF", "Afghanistan");
		COUNTRIES.put("AL", "Albania");
		COUNTRIES.put("DZ", "Algeria");
		COUNTRIES.put("AS", "American Samoa");
		COUNTRIES.put("AD", "Andorra");
		COUNTRIES.put("AO", "Angola");
		COUNTRIES.put("AI", "Anguilla ");
		COUNTRIES.put("AQ", "Antarctica");
		COUNTRIES.put("AG", "Antigua and Barbuda");
		COUNTRIES.put("AR", "Argentina");
		COUNTRIES.put("AM", "Armenia");
		COUNTRIES.put("AW", "Aruba");
		COUNTRIES.put("AU", "Australia");
		COUNTRIES.put("AT", "Austria");
		COUNTRIES.put("AZ", "Azerbaijan");
		COUNTRIES.put("BS", "Bahamas");
		COUNTRIES.put("BH", "Bahrain");
		COUNTRIES.put("BD", "Bangladesh");
		COUNTRIES.put("BB", "Barbados");
		COUNTRIES.put("BY", "Belarus");
		COUNTRIES.put("BE", "Belgium");
		COUNTRIES.put("BZ", "Belize");
		COUNTRIES.put("BJ", "Benin");
		COUNTRIES.put("BM", "Bermuda");
		COUNTRIES.put("BT", "Bhutan");
		COUNTRIES.put("BO", "'Bolivia, Plurinational State Of'  ");
		COUNTRIES.put("BQ", "'Bonaire, Saint Eustatius and Saba'");
		COUNTRIES.put("BA", "Bosnia and Herzegovina");
		COUNTRIES.put("BW", "Botswana");
		COUNTRIES.put("BV", "Bouvet Island");
		COUNTRIES.put("BR", "Brazil");
		COUNTRIES.put("IO", "British Indian Ocean Territory");
		COUNTRIES.put("BN", "Brunei Darussalam");
		COUNTRIES.put("BG", "Bulgaria");
		COUNTRIES.put("BF", "Burkina Faso");
		COUNTRIES.put("BI", "Burundi");
		COUNTRIES.put("KH", "Cambodia");
		COUNTRIES.put("CM", "Cameroon");
		COUNTRIES.put("CA", "Canada");
		COUNTRIES.put("CV", "Cape Verde  ");
		COUNTRIES.put("KY", "Cayman Islands");
		COUNTRIES.put("CF", "Central African Republic");
		COUNTRIES.put("TD", "Chad");
		COUNTRIES.put("CL", "Chile");
		COUNTRIES.put("CN", "China");
		COUNTRIES.put("CX", "Christmas Island");
		COUNTRIES.put("CC", "Cocos (Keeling) Islands");
		COUNTRIES.put("CO", "Colombia");
		COUNTRIES.put("KM", "Comoros");
		COUNTRIES.put("CG", "Congo");
		COUNTRIES.put("CD", "'Congo, The Democratic Republic Of The'");
		COUNTRIES.put("CK", "Cook Islands");
		COUNTRIES.put("CR", "Costa Rica");
		COUNTRIES.put("HR", "Croatia");
		COUNTRIES.put("CU", "Cuba");
		COUNTRIES.put("CW", "Curaçao");
		COUNTRIES.put("CY", "Cyprus");
		COUNTRIES.put("CZ", "Czech Republic");
		COUNTRIES.put("CI", "Côte D\'Ivoire");
		COUNTRIES.put("DK", "Denmark");
		COUNTRIES.put("DJ", "Djibouti");
		COUNTRIES.put("DM", "Dominica");
		COUNTRIES.put("DO", "Dominican Republic");
		COUNTRIES.put("EC", "Ecuador");
		COUNTRIES.put("EG", "Egypt");
		COUNTRIES.put("SV", "El Salvador");
		COUNTRIES.put("GQ", "Equatorial Guinea");
		COUNTRIES.put("ER", "Eritrea");
		COUNTRIES.put("EE", "Estonia");
		COUNTRIES.put("ET", "Ethiopia");
		COUNTRIES.put("FK", "Falkland Islands  (Malvinas)");
		COUNTRIES.put("FO", "Faroe Islands");
		COUNTRIES.put("FJ", "Fiji");
		COUNTRIES.put("FI", "Finland");
		COUNTRIES.put("FR", "France");
		COUNTRIES.put("GF", "French Guiana");
		COUNTRIES.put("PF", "French Polynesia");
		COUNTRIES.put("TF", "French Southern Territories");
		COUNTRIES.put("GA", "Gabon");
		COUNTRIES.put("GM", "Gambia");
		COUNTRIES.put("GE", "Georgia");
		COUNTRIES.put("DE", "Germany");
		COUNTRIES.put("GH", "Ghana ");
		COUNTRIES.put("GI", "Gibraltar");
		COUNTRIES.put("GR", "Greece");
		COUNTRIES.put("GL", "Greenland");
		COUNTRIES.put("GD", "Grenada");
		COUNTRIES.put("GP", "Guadeloupe");
		COUNTRIES.put("GU", "Guam");
		COUNTRIES.put("GT", "Guatemala");
		COUNTRIES.put("GG", "Guernsey");
		COUNTRIES.put("GN", "Guinea");
		COUNTRIES.put("GW", "Guinea-Bissau");
		COUNTRIES.put("GY", "Guyana");
		COUNTRIES.put("HT", "Haiti");
		COUNTRIES.put("HM", "Heard and McDonald Islands");
		COUNTRIES.put("VA", "Holy See (Vatican City State)");
		COUNTRIES.put("HN", "Honduras");
		COUNTRIES.put("HK", "Hong Kong");
		COUNTRIES.put("HU", "Hungary");
		COUNTRIES.put("IS", "Iceland");
		COUNTRIES.put("IN", "India");
		COUNTRIES.put("ID", "Indonesia");
		COUNTRIES.put("IR", "'Iran, Islamic Republic Of'");
		COUNTRIES.put("IQ", "Iraq");
		COUNTRIES.put("IE", "Ireland");
		COUNTRIES.put("IM", "Isle of Man");
		COUNTRIES.put("IL", "Israel");
		COUNTRIES.put("IT", "Italy");
		COUNTRIES.put("JM", "Jamaica");
		COUNTRIES.put("JP", "Japan");
		COUNTRIES.put("JE", "Jersey");
		COUNTRIES.put("JO", "Jordan");
		COUNTRIES.put("KZ", "Kazakhstan");
		COUNTRIES.put("KE", "Kenya");
		COUNTRIES.put("KI", "Kiribati");
		COUNTRIES.put("KP", "'Korea, Democratic People\'s Republic Of'");
		COUNTRIES.put("KR", "'Korea, Republic of'");
		COUNTRIES.put("KW", "Kuwait");
		COUNTRIES.put("KG", "Kyrgyzstan");
		COUNTRIES.put("LA", "Lao People\'s Democratic Republic");
		COUNTRIES.put("LV", "Latvia");
		COUNTRIES.put("LB", "Lebanon");
		COUNTRIES.put("LS", "Lesotho");
		COUNTRIES.put("LR", "Liberia");
		COUNTRIES.put("LY", "Libyan Arab Jamahiriya");
		COUNTRIES.put("LI", "Liechtenstein");
		COUNTRIES.put("LT", "Lithuania");
		COUNTRIES.put("LU", "Luxembourg");
		COUNTRIES.put("MO", "Macao");
		COUNTRIES.put("MK", "'Macedonia, the Former Yugoslav Republic Of'");
		COUNTRIES.put("MG", "Madagascar");
		COUNTRIES.put("MW", "Malawi");
		COUNTRIES.put("MY", "Malaysia");
		COUNTRIES.put("MV", "Maldives");
		COUNTRIES.put("ML", "Mali");
		COUNTRIES.put("MT", "Malta");
		COUNTRIES.put("MH", "Marshall Islands");
		COUNTRIES.put("MQ", "Martinique");
		COUNTRIES.put("MR", "Mauritania");
		COUNTRIES.put("MU", "Mauritius");
		COUNTRIES.put("YT", "Mayotte");
		COUNTRIES.put("MX", "Mexico");
		COUNTRIES.put("FM", "'Micronesia, Federated States Of'");
		COUNTRIES.put("MD", "'Moldova, Republic of'");
		COUNTRIES.put("MC", "Monaco");
		COUNTRIES.put("MN", "Mongolia");
		COUNTRIES.put("ME", "Montenegro");
		COUNTRIES.put("MS", "Montserrat");
		COUNTRIES.put("MA", "Morocco");
		COUNTRIES.put("MZ", "Mozambique");
		COUNTRIES.put("MM", "Myanmar");
		COUNTRIES.put("NA", "Namibia");
		COUNTRIES.put("NR", "Nauru");
		COUNTRIES.put("NP", "Nepal");
		COUNTRIES.put("NL", "Netherlands");
		COUNTRIES.put("AN", "Netherlands Antilles");
		COUNTRIES.put("NC", "New Caledonia");
		COUNTRIES.put("NZ", "New Zealand");
		COUNTRIES.put("NI", "Nicaragua");
		COUNTRIES.put("NE", "Niger");
		COUNTRIES.put("NG", "Nigeria");
		COUNTRIES.put("NU", "Niue");
		COUNTRIES.put("NF", "Norfolk Island");
		COUNTRIES.put("MP", "Northern Mariana Islands");
		COUNTRIES.put("NO", "Norway");
		COUNTRIES.put("OM", "Oman");
		COUNTRIES.put("PK", "Pakistan");
		COUNTRIES.put("PW", "Palau");
		COUNTRIES.put("PS", "'Palestinian Territory, Occupied'");
		COUNTRIES.put("PA", "Panama");
		COUNTRIES.put("PG", "Papua New Guinea");
		COUNTRIES.put("PY", "Paraguay");
		COUNTRIES.put("PE", "Peru");
		COUNTRIES.put("PH", "Philippines");
		COUNTRIES.put("PN", "Pitcairn");
		COUNTRIES.put("PL", "Poland");
		COUNTRIES.put("PT", "Portugal");
		COUNTRIES.put("PR", "Puerto Rico");
		COUNTRIES.put("QA", "Qatar");
		COUNTRIES.put("RO", "Romania");
		COUNTRIES.put("RU", "Russian Federation");
		COUNTRIES.put("RW", "Rwanda");
		COUNTRIES.put("RE", "Réunion");
		COUNTRIES.put("BL", "Saint Barthélemy");
		COUNTRIES.put("SH", "'Saint Helena, Ascension and Tristan Da Cunha'");
		COUNTRIES.put("KN", "Saint Kitts And Nevis");
		COUNTRIES.put("LC", "Saint Lucia");
		COUNTRIES.put("MF", "Saint Martin");
		COUNTRIES.put("PM", "Saint Pierre And Miquelon");
		COUNTRIES.put("VC", "Saint Vincent And The Grenedines");
		COUNTRIES.put("WS", "Samoa");
		COUNTRIES.put("SM", "San Marino");
		COUNTRIES.put("ST", "Sao Tome and Principe");
		COUNTRIES.put("SA", "Saudi Arabia");
		COUNTRIES.put("SN", "Senegal");
		COUNTRIES.put("RS", "Serbia");
		COUNTRIES.put("SC", "Seychelles");
		COUNTRIES.put("SL", "Sierra Leone");
		COUNTRIES.put("SG", "Singapore");
		COUNTRIES.put("SX", "Sint Maarten (Dutch part)");
		COUNTRIES.put("SK", "Slovakia");
		COUNTRIES.put("SI", "Slovenia");
		COUNTRIES.put("SB", "Solomon Islands");
		COUNTRIES.put("SO", "Somalia");
		COUNTRIES.put("ZA", "South Africa");
		COUNTRIES.put("GS", "South Georgia and the South Sandwich Islands");
		COUNTRIES.put("SS", "South Sudan");
		COUNTRIES.put("ES", "Spain");
		COUNTRIES.put("LK", "Sri Lanka");
		COUNTRIES.put("SD", "Sudan");
		COUNTRIES.put("SR", "Suriname");
		COUNTRIES.put("SJ", "Svalbard And Jan Mayen");
		COUNTRIES.put("SZ", "Swaziland");
		COUNTRIES.put("SE", "Sweden");
		COUNTRIES.put("CH", "Switzerland");
		COUNTRIES.put("SY", "Syrian Arab Republic  ");
		COUNTRIES.put("TW", "'Taiwan, Province Of China'");
		COUNTRIES.put("TJ", "Tajikistan");
		COUNTRIES.put("TZ", "'Tanzania, United Republic of'");
		COUNTRIES.put("TH", "Thailand");
		COUNTRIES.put("TL", "Timor-Leste");
		COUNTRIES.put("TG", "Togo");
		COUNTRIES.put("TK", "Tokelau");
		COUNTRIES.put("TO", "Tonga");
		COUNTRIES.put("TT", "Trinidad and Tobago");
		COUNTRIES.put("TN", "Tunisia");
		COUNTRIES.put("TR", "Turkey");
		COUNTRIES.put("TM", "Turkmenistan");
		COUNTRIES.put("TC", "Turks and Caicos Islands");
		COUNTRIES.put("TV", "Tuvalu");
		COUNTRIES.put("UG", "Uganda");
		COUNTRIES.put("UA", "Ukraine");
		COUNTRIES.put("AE", "United Arab Emirates");
		COUNTRIES.put("GB", "United Kingdom");
		COUNTRIES.put("US", "United States");
		COUNTRIES.put("UM", "United States Minor Outlying Islands");
		COUNTRIES.put("UY", "Uruguay");
		COUNTRIES.put("UZ", "Uzbekistan");
		COUNTRIES.put("VU", "Vanuatu");
		COUNTRIES.put("VE", "'Venezuela, Bolivarian Republic of' ");
		COUNTRIES.put("VN", "Viet Nam");
		COUNTRIES.put("VG", "'Virgin Islands, British'");
		COUNTRIES.put("VI", "'Virgin Islands, U.S.'");
		COUNTRIES.put("WF", "Wallis and Futuna");
		COUNTRIES.put("EH", "Western Sahara");
		COUNTRIES.put("YE", "Yemen");
		COUNTRIES.put("ZM", "Zambia");
		COUNTRIES.put("ZW", "Zimbabwe");
		COUNTRIES.put("AX", "Åland Islands");
	}

	private CountryCodes()
	{
	}

	public static Map<String, String> get()
	{
		// returning Collections.unmodifiableMap results in Freemarker not respecting map iteration order
		return COUNTRIES;
	}

	public static String get(String countryCode)
	{
		return COUNTRIES.get(countryCode);
	}
}