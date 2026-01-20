package dev.lucasschotttler.lakes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import dev.lucasschotttler.update.Ebay;

@Service
public class Lakes {
    
    private static final Logger logger = LoggerFactory.getLogger(Ebay.class);
    private static final String ITEMLINK = "https://swymstore-v3pro-01.swymrelay.com/api/v2/provider/getPlatformProducts?pid=jn9XxHMVJRoc160vy%2BI3OVpfL8Wq3P19N1qklE2GjTk%3D";
    private static final String APILINK = "https://searchserverapi1.com/getresults?api_key=4O3Y4Q0o6o";

    // #region TOOL_NAMES
    private static final List<String> TOOL_NAMES = Arrays.asList(
        "drill bit", "hammer", "ruler", "rule", "auger", "wrench", "grease", "flange",
        "head kit", "grinding guard", "die", "tip kit", "bit", "wheel", "blade", "tpi",
        "sleeve", "saw kit", "pin", "power source", "screwdriver", "chisel", "battery",
        "crimper", "spring", "snips", "electronics assembly", "split ring", "tamper plate",
        "head cable", "nut driver", "jaw pliers", "chainsaw kit", "ball brg", "organizer",
        "tubing cutter", "torque impact", "light", "cooler", "inverter", "guard", "shaft",
        "gloves", "tube", "handle", "chuck", "padded rig", "screw accessory kit",
        "screw accessory set", "brush set", "valves blower", "brad point", "belt", "pouch",
        "lanyard", "tape base", "cord", "expansion head", "screw", "clamp", "ratchet", "saw",
        "brush and holder assembly", "o-ring", "cable", "charger", "cap", "brush", "assembly",
        "nozzle", "gun", "rotor housing kit", "palm nailer", "punch", "nailer", "rang",
        "gas lift kit", "knife", "pliers", "crimp jaw", "field assy", "o ring", "foam insert",
        "tape measure", "helix", "socket adapter", "chalk", "inflator", "jaw", "crevice tool",
        "tinner", "inspection kit", "button kit", "knockout set", "housing connector",
        "fork meter", "bull point", "pulley and washer", "plate", "router", "bag", "switch",
        "marker", "insulator", "ring", "guide", "shear", "tool box", "tester",
        "voltage detector", "clip", "gear finished", "angular cutter", "tape", "line",
        "bushing", "shroud", "stud", "thunderbolt", "accessory", "extension",
        "carbide teeth", "scraper", "fence", "strain relief", "adaptor", "cutter", "meter",
        "washer", "towel", "backpack", "sds-max", "knockout tool", "do not enter", "bucket",
        "ball pull", "spade", "nut", "pipe threader", "level", "gear", "knockout", "snip",
        "stapler", "pick", "hose", "pry bar", "bolt", "floor tool", "driver", "fish kit",
        "benders", "impact", "pan head", "shockwave", "coupling", "expansion tool kit", "file",
        "cut out tool", "barrel kit", "pusher", "sds+", "backing pad", "caulk", "angle grinder",
        "armature", "collar", "housing kit", "bumper", "service field", "cam", "strap", "lever",
        "anvil service kit", "tote", "bottle opener", "t-block", "adapter", "charge station",
        "cut off tool kit", "shank", "rotor kit", "motor housing", "press tool kit", "latch kit",
        "axle service kit", "rivet tool", "gasket", "v-block", "hawg", "finishing pad", "shirt",
        "magazine", "test lead set", "tray", "cleaning set", "arbor", "carriage conversion kit",
        "collet", "shuttle", "dimpler", "placement tool", "expansion kit", "square socket",
        "socket", "vacuum", "tumbler", "gauge", "paddle", "face mask", "hook", "packout set",
        "knob female thread", "baffle", "hat", "laser kit", "packout", "sds max", "respirator",
        "nail puller", "service kit", "usb/aux door", "knee pad", "sds-plus", "fish stick kit",
        "hackzall", "rivet nose pierce", "long throw press tool", "grout removal tool",
        "stripper", "cut-off machine", "tool boot", "headlamp", "speaker",
        "bulb head attachment", "angler", "rivet nose piece", "m12", "iron press",
        "conversion kit", "blower", "hole dozer hole", "sdsmax", "crown stop", "v field", "atb",
        "test leads", "wool cutting pad", "pulley service tool", "framer", "press tool",
        "glazer bar", "test probes", "trim square", "fish stick", "contractors kit", "hood",
        "chain hoist", "trim puller", "barricade", "cover", "extraction tool", "hex", "sheath",
        "electronics module", "funnel head", "field service", "polishing pad", "umbrella",
        "extractor", "reaming pen", "annular kit", "key", "roller", "electricians kit",
        "tool rest", "filter", "pump", "holder", "flap disc", "depth stop", "plumbers kit",
        "pulley", "knuckle", "sheet palm sander", "masonry cut-off", "thermocouple",
        "square recess", "stool", "tapt", "edger attachment", "antenna", "fiberglass",
        "contractor pack", "window box", "smooth face", "grinder kit", "face", "case", "kit",
        "planer", "installation", "polisher", "sub-base", "grip", "seal", "field", "attachment",
        "trimmer", "wand", "coupler", "seamer", "apron", "pen", "square", "frame", "sprocket",
        "scabbard", "glasses", "vest", "compressor", "extraction", "drive", "countersink",
        "plugs", "rod", "turbo", "set", "chute", "exhaust", "pad", "sanding", "paper",
        "sandpaper", "multi-tool", "knob", "power supply", "bracket", "target", "mechanics",
        "bar", "laser", "heads", "rivet", "nose piece", "replacement", "ball", "bearing", "brg",
        "shelves", "expander", "breaker", "universal joint", "tripod", "utility", "grinder",
        "steel", "opn", "mount", "slide", "pole", "torx", "tap", "terminal", "insert", "clutch",
        "button", "slug", "valve", "gib", "flathead", "variable speed", "braking", "rotary",
        "collector", "vibrator", "stick", "scissors", "variety", "head", "output", "starter",
        "tool", "broom", "hanger", "carrying", "axe", "radio", "ear buds", "forge", "dual bay",
        "magnetic", "motor", "shift", "wall mount", "combo", "port", "concrete", "remote",
        "tablet", "visor", "lenses", "coax", "yoke", "redefiner", "reciprocator", "hedge",
        "bed", "collated", "lens", "mag", "molding", "puller", "trimbone", "cut-off", "pivoting",
        "post", "barrier", "short head", "rubber head", "framing", "finish", "mesh", "discs",
        "saver", "interface", "sponge", "finder", "boot", "redlithium", "duplex", "nails",
        "foam", "segmented", "general purpose", "steelhead", "carpenter", "pencil",
        "playing cards", "empire", "rafter", "hollowcore", "stator", "pcba", "power head",
        "quik-lok", "pommel", "housing", "dovetail", "rail", "label", "spacer", "shoe", "chain",
        "supporting", "brake", "drawer", "carriage", "sweep", "motor frame", "axle",
        "short pole", "fitting", "lock", "plastic", "impeller", "window", "caster", "infinity",
        "kneeling", "power manager", "circuit", "water supply", "nibbler", "chug", "lid",
        "insulated", "bottle", "grout removal", "grit", "wireless", "dust control", "aluminum",
        "conduit", "bender", "sanding discs", "redefinisher", "sprayer", "tank", "pruner",
        "wireless remote", "lopper", "flexible", "cuff", "jump starter", "hotshot", "brass",
        "catch", "hardware", "fixed", "adjustable", "overload", "protector", "catcher", "miter",
        "base", "fan", "bladder", "mulch", "plug", "scrench", "spur", "oil", "leg", "connector",
        "selector", "no mar tip", "anvil", "counter balance", "wire", "pully", "ram",
        "anti-splinter strip", "non-slip strip", "reamer cone", "module", "mach scr", "wrecker",
        "nitrus", "assembled foot", "piston", "tibone", "titanium", "cartridge carrige",
        "wrecking work glove", "edger", "coin cell board", "rubber foot", "lower control board",
        "led", "latch", "inkzall", "nose", "pan", "dust shield", "rubber cushion", "flat wash",
        "anniversary", "drill"
    );
    // #endregion

    public static String getToolType(String title) {
        if (title == null) return "";
        
        String lowerTitle = title.toLowerCase();
        String longestTool = "";
        
        for (String tool : TOOL_NAMES) {
            if (lowerTitle.contains(tool)) {
                if (tool.length() > longestTool.length()) {
                    longestTool = tool;
                }
            }
        }
        
        return longestTool;
    }

    public static LakesItem getLakesItem(int id) {
        try {
            String payload = "productids=%5B" + id + "%5D&regid=JXNnJGEEgrP63HI0SQEsMlT-lqGvpin-gs-TMt4v7KZNhXb8BXV3AGU9VvweaaoRkXWjtvc25shVAKa5Zb5MaI2GlIuSXzYIpfEQ-l87Y2qaaVGq2dRdEzHBjvkTweZeZjjfPDycP_6LDolPIapsKuHskOVMaGSnI_JsLF20Py8&sessionid=qtwf2nkbfh6bbs8f9c4u8ux434l4vhnlb34okc9a675gabmmigagcv6ojyu8ou04";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(ITEMLINK))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .headers(
                    "Content-Type", "application/x-www-form-urlencoded",
                    "Accept", "application/json",
                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                );

            HttpRequest request = requestBuilder.build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("Response status: " + response.statusCode());
            logger.info("Response body: " + response.body());

            LakesItem lakesItem = new LakesItem(response.body());

            return lakesItem;
        } catch (Exception e) {
            logger.error("Error during POST request", e);
        }
        return null;
    }
}