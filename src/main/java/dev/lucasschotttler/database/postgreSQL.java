package dev.lucasschotttler.database;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.lucasschotttler.lakesAPI.Lakes;
import dev.lucasschotttler.lakesAPI.Lakes.Item;
import dev.lucasschotttler.lakesAPI.Lakes.LakesReturn;
import tools.jackson.databind.ObjectMapper;

@Repository
public class postgreSQL {
    
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
      
    public postgreSQL(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getToolType(String title) {
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
    
    public String createEntries() throws IOException, InterruptedException{
    int startIndex = 0;
    int numItems = 100000;
    Lakes lakes = new Lakes(); // Create Lakes instance once

    while (startIndex < numItems) {
        String linkPage = Lakes.getLakesAPILink() + "&startIndex=" + startIndex + "&maxResults=250&items=true&pages=true&categories=true";
        startIndex += 250;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(linkPage))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        LakesReturn data = objectMapper.readValue(response.body(), LakesReturn.class);
        
        List<Item> items = data.getItems();
        numItems = data.getTotalItems();
        
        for (Item item : items) {
            Integer lakesId = Integer.parseInt(item.getProduct_id());
            
            String checkSql = "SELECT COUNT(*) FROM superior WHERE lakesid = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, lakesId);
            
            if (count != null && count > 0) {
                continue;
            }
            
            Item detailedItem = lakes.getLakesItem(item.getProduct_id());
            
            if (detailedItem == null) {
                continue;
            }
            
            // Skip items without SKU (required field)
            if (detailedItem.getSku() == null || detailedItem.getSku().trim().isEmpty()) {
                System.out.println("Skipping item " + lakesId + " - missing SKU");
                continue;
            }
            
            String toolType = getToolType(detailedItem.getTitle());
            
            String sql = "INSERT INTO superior (lakesid, width, length, height, weight, type, mpn, title, description, upc, brand, quantity, sku, name, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            jdbcTemplate.update(sql,
                lakesId,
                detailedItem.getWidth(),
                detailedItem.getLength(),
                detailedItem.getHeight(),
                detailedItem.getWeight(),
                toolType,
                detailedItem.getProduct_code(),
                detailedItem.getTitle(),
                detailedItem.getDescription(),
                detailedItem.getUpc(),
                detailedItem.getBrand(),
                detailedItem.getQuantity() != null ? detailedItem.getQuantity() : 0,
                detailedItem.getSku(),
                detailedItem.getName()
            );
        }
    }

    return "Success";
}
    
    public List<String> getImages(String tableName, String SKU) {
        String sql = "SELECT web_api FROM " + tableName + " WHERE filename LIKE ?";
        String pattern = "%" + SKU + "%";
        return jdbcTemplate.queryForList(sql, String.class, pattern);
    }
}