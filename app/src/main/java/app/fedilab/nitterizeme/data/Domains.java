package app.fedilab.nitterizeme.data;

public class Domains {
    //Supported domains
    public static String[] twitter_domains = {
            "twitter.com",
            "mobile.twitter.com",
            "www.twitter.com",
            "pbs.twimg.com",
            "pic.twitter.com"
    };
    public static String[] instagram_domains = {
            "instagram.com",
            "www.instagram.com",
            "m.instagram.com",
    };
    public static String[] youtube_domains = {
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com",
            "youtu.be",
            "youtube-nocookie.com"
    };
    public static String[] shortener_domains = {
            "t.co",
            "nyti.ms",
            "bit.ly",
            "amp.gs",
            "tinyurl.com",
            "goo.gl",
            "nzzl.us",
            "ift.tt",
            "ow.ly",
            "bl.ink",
            "buff.ly",
            "maps.app.goo.gl"
    };
    //Supported instances to redirect one instance to another faster for the user
    public static String[] invidious_instances = {
            "invidio.us",
            "invidious.snopyta.org",
            "invidiou.sh",
            "invidious.toot.koeln",
            "invidious.ggc-project.de",
            "invidious.13ad.de",
            "yewtu.be"
    };
    public static String[] nitter_instances = {
            "nitter.net",
            "nitter.snopyta.org",
            "nitter.42l.fr",
            "nitter.13ad.de",
            "tw.openalgeria.org",
            "nitter.pussthecat.org",
            "nitter.mastodont.cat",
            "nitter.dark.fail",
            "nitter.tedomum.net"
    };
    public static String[] bibliogram_instances = {
            "bibliogram.art",
            "bibliogram.snopyta.org",
            "bibliogram.dsrev.ru",
            "bibliogram.pussthecat.org"
    };

    public static String outlook_safe_domain = "safelinks.protection.outlook.com";
}