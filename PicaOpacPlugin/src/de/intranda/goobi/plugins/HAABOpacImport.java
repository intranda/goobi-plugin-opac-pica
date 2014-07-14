/**
 * This file is part of the pica opac import plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://digiverso.com 
 *          - http://www.intranda.com
 * 
 * Copyright 2011 - 2013, intranda GmbH, Göttingen
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */
package de.intranda.goobi.plugins;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IOpacPlugin;
import org.jdom2.Element;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Prefs;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import de.sub.goobi.helper.UghHelper;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;

@PluginImplementation
public class HAABOpacImport extends PicaOpacImport implements IOpacPlugin {

    private String atstsl;
    ConfigOpacCatalogue coc;

    /* (non-Javadoc)
     * @see de.sub.goobi.Import.IOpac#createAtstsl(java.lang.String, java.lang.String)
     */

    public String createAtstsl(String myTitle, String autor) {
        String titleValue = "";
        if (myTitle != null && !myTitle.isEmpty()) {
            if (myTitle.contains(" ")) {
                titleValue = myTitle.substring(0, myTitle.indexOf(" "));
            } else {
                titleValue = myTitle;
            }
        }
        String myAtsTsl = "";

        if (titleValue.length() > 6) {
            myAtsTsl = titleValue.substring(0, 6);
        } else {
            myAtsTsl = titleValue;
        }

        myAtsTsl = myAtsTsl.replaceAll("[\\W]", "");
        return myAtsTsl.toLowerCase();
    }

    /* (non-Javadoc)
     * @see de.sub.goobi.Import.IOpac#getAtstsl()
     */
    @Override
    public String getAtstsl() {
        return this.atstsl;
    }

    @Override
    public PluginType getType() {
        return PluginType.Opac;
    }

    @Override
    public String getTitle() {
        return "HAAB";
    }

    @Override
    public String getDescription() {
        return "HAAB";
    }

    public void checkMyOpacResult(DigitalDocument inDigDoc, Prefs inPrefs, Element myFirstHit, boolean verbose) {
        UghHelper ughhelp = new UghHelper();
        DocStruct topstruct = inDigDoc.getLogicalDocStruct();
        DocStruct boundbook = inDigDoc.getPhysicalDocStruct();
        DocStruct topstructChild = null;
        Element mySecondHit = null;

        /*
         * -------------------------------- bei Multivolumes noch das Child in xml und docstruct ermitteln --------------------------------
         */
        // if (isMultivolume()) {
        if (getOpacDocType().isMultiVolume()) {
            try {
                topstructChild = topstruct.getAllChildren().get(0);
            } catch (RuntimeException e) {
            }
            mySecondHit = (Element) myFirstHit.getParentElement().getChildren().get(1);
        }

        /*
         * -------------------------------- vorhandene PPN als digitale oder analoge einsetzen --------------------------------
         */
        String ppn = getElementFieldValue(myFirstHit, "003@", "0");
        ughhelp.replaceMetadatum(topstruct, inPrefs, "CatalogIDDigital", "");
        if (gattung.toLowerCase().startsWith("o")) {
            ughhelp.replaceMetadatum(topstruct, inPrefs, "CatalogIDDigital", ppn);
        } else {
            ughhelp.replaceMetadatum(topstruct, inPrefs, "CatalogIDSource", ppn);
        }

        /*
         * -------------------------------- wenn es ein multivolume ist, dann auch die PPN prüfen --------------------------------
         */
        if (topstructChild != null && mySecondHit != null) {
            String secondHitppn = getElementFieldValue(mySecondHit, "003@", "0");
            ughhelp.replaceMetadatum(topstructChild, inPrefs, "CatalogIDDigital", "");
            if (this.gattung.toLowerCase().startsWith("o")) {
                ughhelp.replaceMetadatum(topstructChild, inPrefs, "CatalogIDDigital", secondHitppn);
            } else {
                ughhelp.replaceMetadatum(topstructChild, inPrefs, "CatalogIDSource", secondHitppn);
            }
        }

        /*
         * -------------------------------- den Main-Title bereinigen --------------------------------
         */
        String myTitle = getElementFieldValue(myFirstHit, "021A", "a");
        /*
         * wenn der Fulltittle nicht in dem Element stand, dann an anderer Stelle nachsehen (vor allem bei Contained-Work)
         */
        if (myTitle == null || myTitle.length() == 0) {
            myTitle = getElementFieldValue(myFirstHit, "021B", "a");
        }
        ughhelp.replaceMetadatum(topstruct, inPrefs, "TitleDocMain", myTitle.replaceAll("@", ""));

        /*
         * -------------------------------- Sorting-Titel mit Umlaut-Konvertierung --------------------------------
         */
        if (myTitle.indexOf("@") != -1) {
            myTitle = myTitle.substring(myTitle.indexOf("@") + 1);
        }
        ughhelp.replaceMetadatum(topstruct, inPrefs, "TitleDocMainShort", myTitle);

        /*
         * -------------------------------- bei multivolumes den Main-Title bereinigen --------------------------------
         */
        if (topstructChild != null && mySecondHit != null) {
            String fulltitleMulti = getElementFieldValue(mySecondHit, "021A", "a").replaceAll("@", "");
            ughhelp.replaceMetadatum(topstructChild, inPrefs, "TitleDocMain", fulltitleMulti);
        }

        /*
         * -------------------------------- bei multivolumes den Sorting-Titel mit Umlaut-Konvertierung --------------------------------
         */
        if (topstructChild != null && mySecondHit != null) {
            String sortingTitleMulti = getElementFieldValue(mySecondHit, "021A", "a");
            if (sortingTitleMulti.indexOf("@") != -1) {
                sortingTitleMulti = sortingTitleMulti.substring(sortingTitleMulti.indexOf("@") + 1);
            }
            ughhelp.replaceMetadatum(topstructChild, inPrefs, "TitleDocMainShort", sortingTitleMulti);
            // sortingTitle = sortingTitleMulti;
        }

        /*
         * -------------------------------- Sprachen - Konvertierung auf zwei Stellen --------------------------------
         */
        String sprache = getElementFieldValue(myFirstHit, "010@", "a");
        sprache = ughhelp.convertLanguage(sprache);
        ughhelp.replaceMetadatum(topstruct, inPrefs, "DocLanguage", sprache);

        /*
         * -------------------------------- bei multivolumes die Sprachen - Konvertierung auf zwei Stellen --------------------------------
         */
        if (topstructChild != null && mySecondHit != null) {
            String spracheMulti = getElementFieldValue(mySecondHit, "010@", "a");
            spracheMulti = ughhelp.convertLanguage(spracheMulti);
            ughhelp.replaceMetadatum(topstructChild, inPrefs, "DocLanguage", spracheMulti);
        }

        /*
         * -------------------------------- ISSN --------------------------------
         */
        String issn = getElementFieldValue(myFirstHit, "005A", "0");
        ughhelp.replaceMetadatum(topstruct, inPrefs, "ISSN", issn);

        /*
         * -------------------------------- Copyright --------------------------------
         */
        String copyright = getElementFieldValue(myFirstHit, "037I", "a");
        ughhelp.replaceMetadatum(boundbook, inPrefs, "copyrightimageset", copyright);

        /*
         * -------------------------------- Format --------------------------------
         */
        String format = getElementFieldValue(myFirstHit, "034I", "a");
        ughhelp.replaceMetadatum(boundbook, inPrefs, "FormatSourcePrint", format);

        /*
         * -------------------------------- Umfang --------------------------------
         */
        String umfang = getElementFieldValue(myFirstHit, "034D", "a");
        ughhelp.replaceMetadatum(topstruct, inPrefs, "SizeSourcePrint", umfang);

        /*
         * -------------------------------- Signatur --------------------------------
         */
        String sig = getElementFieldValue(myFirstHit, "209A", "c");
        if (sig.length() > 0) {
            sig = "<" + sig + ">";
        }
        sig += getElementFieldValue(myFirstHit, "209A", "f") + " ";
        sig += getElementFieldValue(myFirstHit, "209A", "a");
        ughhelp.replaceMetadatum(boundbook, inPrefs, "shelfmarksource", sig.trim());
        if (sig.trim().length() == 0) {
            myLogger.debug("Signatur part 1: " + sig);
            myLogger.debug(myFirstHit.getChildren());
            sig = getElementFieldValue(myFirstHit, "209A/01", "c");
            if (sig.length() > 0) {
                sig = "<" + sig + ">";
            }
            sig += getElementFieldValue(myFirstHit, "209A/01", "f") + " ";
            sig += getElementFieldValue(myFirstHit, "209A/01", "a");
            if (mySecondHit != null) {
                sig += getElementFieldValue(mySecondHit, "209A", "f") + " ";
                sig += getElementFieldValue(mySecondHit, "209A", "a");
            }
            ughhelp.replaceMetadatum(boundbook, inPrefs, "shelfmarksource", sig.trim());
        }
        myLogger.debug("Signatur full: " + sig);

        /*
         * -------------------------------- Ats Tsl Vorbereitung --------------------------------
         */
        myTitle = myTitle.toLowerCase();
        myTitle = myTitle.replaceAll("&", "");

        /*
         * -------------------------------- bei nicht-Zeitschriften Ats berechnen --------------------------------
         */
        // if (!gattung.startsWith("ab") && !gattung.startsWith("ob")) {
        String autor = getElementFieldValue(myFirstHit, "028A", "a").toLowerCase();
        if (autor == null || autor.equals("")) {
            autor = getElementFieldValue(myFirstHit, "028A", "8").toLowerCase();
        }
        this.atstsl = createAtstsl(myTitle, autor);

        /*
         * -------------------------------- bei Zeitschriften noch ein PeriodicalVolume als Child einfügen --------------------------------
         */
        // if (isPeriodical()) {
        if (getOpacDocType().isPeriodical()) {
            try {
                DocStructType dstV = inPrefs.getDocStrctTypeByName("PeriodicalVolume");
                DocStruct dsvolume = inDigDoc.createDocStruct(dstV);
                topstruct.addChild(dsvolume);
            } catch (TypeNotAllowedForParentException e) {
                e.printStackTrace();
            } catch (TypeNotAllowedAsChildException e) {
                e.printStackTrace();
            }
        }

    }
}
