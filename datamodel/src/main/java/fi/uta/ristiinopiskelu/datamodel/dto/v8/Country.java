package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tilastokeskus: https://www2.tilastokeskus.fi/fi/luokitukset/valtio/
 */
public enum Country {

    AF("004"), AX("248"), NL("528"), AL("008"), DZ("012"), AS("016"), AD("020"), AO("024"), AI("660"), AQ("010"), AG("028"),
    AE("784"), AR("032"), AM("051"), AW("533"), AU("036"), AZ("031"), BS("044"), BH("048"), BD("050"), BB("052"), BE("056"),
    BZ("084"), BJ("204"), BM("060"), BT("064"), BO("068"), BQ("535"), BA("070"), BW("072"), BV("074"), BR("076"), GB("826"),
    IO("086"), VG("092"), BN("096"), BG("100"), BF("854"), BI("108"), KY("136"), CL("152"), CK("184"), CR("188"), CW("531"),
    DJ("262"), DM("212"), DO("214"), EC("218"), EG("818"), SV("222"), ER("232"), ES("724"), ZA("710"), GS("239"), SS("728"),
    ET("231"), FK("238"), FJ("242"), PH("608"), FO("234"), GA("266"), GM("270"), GE("268"), GH("288"), GI("292"), GD("308"),
    GL("304"), GP("312"), GU("316"), GT("320"), GG("831"), GN("324"), GW("624"), GY("328"), HT("332"), HM("334"), HN("340"),
    HK("344"), ID("360"), IN("356"), IQ("368"), IR("364"), IE("372"), IS("352"), IL("376"), IT("380"), TL("626"), AT("040"),
    JM("388"), JP("392"), YE("887"), JE("832"), JO("400"), CX("162"), KH("116"), CM("120"), CA("124"), CV("132"), KZ("398"),
    KE("404"), CF("140"), CN("156"), KG("417"), KI("296"), CO("170"), KM("174"), CG("178"), CD("180"), CC("166"), KP("408"),
    KR("410"), GR("300"), HR("191"), CU("192"), KW("414"), CY("196"), LA("418"), LV("428"), LS("426"), LB("422"), LR("430"),
    LY("434"), LI("438"), LT("440"), LU("442"), EH("732"), MO("446"), MG("450"), MK("870"), MW("454"), MV("462"), MY("458"),
    ML("466"), MT("470"), IM("833"), MA("504"), MH("584"), MQ("474"), MR("478"), MU("480"), YT("175"), MX("484"), FM("583"),
    MD("498"), MC("492"), MN("496"), ME("499"), MS("500"), MZ("508"), MM("104"), NA("516"), NR("520"), NP("524"), NI("558"),
    NE("562"), NG("566"), NU("570"), NF("574"), NO("578"), CI("384"), OM("512"), PK("586"), PW("585"), PS("275"), PA("591"),
    PG("598"), PY("600"), PE("604"), PN("612"), MP("580"), PT("620"), PR("630"), PL("616"), GQ("226"), QA("634"), FR("250"),
    TF("260"), GF("254"), PF("258"), RE("638"), RO("642"), RW("646"), SE("752"), BL("652"), SH("654"), KN("659"), LC("662"),
    MF("663"), VC("670"), PM("666"), DE("276"), SB("090"), ZM("894"), WS("882"), SM("674"), ST("678"), SA("682"), SN("686"),
    RS("688"), SC("690"), SL("694"), SG("702"), SX("534"), SK("703"), SI("705"), SO("706"), LK("144"), SD("729"), FI("246"),
    SR("740"), SJ("744"), SZ("748"), CH("756"), SY("760"), TJ("762"), TW("158"), TZ("834"), DK("208"), TH("764"), TG("768"),
    TK("772"), TO("776"), TT("780"), TD("148"), CZ("203"), TN("788"), TR("792"), TM("795"), TC("796"), TV("798"), UG("800"),
    UA("804"), HU("348"), UY("858"), NC("540"), NZ("554"), UZ("860"), BY("112"), WF("876"), VU("548"), VA("336"), VE("862"),
    RU("643"), VN("704"), EE("233"), US("840"), VI("850"), UM("581"), ZW("716");

    private String code;

    Country(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Country fromValue(String code) {
        for (Country c : Country.values()) {
            if (c.code.equalsIgnoreCase(code)) {
                return c;
            }
        }
        return null;
    }

    // Show values correctly in Swagger
    @Override
    public String toString() {
        return this.code;
    }
}
