import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * b站动态抽奖程序
 * 部分实现参考自 https://github.com/LeoChen98/BiliRaffle/blob/master/BiliRaffle/Raffle.cs
 *
 * @author CrazyKid
 * @date 2021/2/7 09:04
 */
public class BilibiliDynamicApiTest {
    /**
     * 动态转发数据获取接口
     */
    private static final String DYNAMIC_REPOST_API = "https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost_detail";

    /**
     * 获取用户的动态接口, %d代表uid
     */
    private static final String USER_DYNAMIC_API = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history?visitor_uid=0&host_uid=%d&offset_dynamic_id=0";

    /**
     * 检测是否关注接口, %d代表uid
     */
    private static final String CHECK_FOLLOW_API = "https://api.bilibili.com/x/space/acc/relation?mid=%d";

    /**
     * b站账号登录cookie, 用于判断转发者是否有关注
     */
    private static final String COOKIE = "";
    /**
     * uid缓存key
     */
    private static final String UIDS_KEY = "bilibiliRepost_uids";

    /**
     * 最后一次获取时间的缓存key
     */
    private static final String LAST_CHECK_TIME_KEY = "bilibiliRepost_lastCheckTime";

    /**
     * 获取间隔(小时)
     */
    private static final int COOLDOWN_HOUR = 1;

    /**
     * 动态id
     */
    private static final long DYNAMIC_ID = 487832633272945539L;

    public static void main(String[] args) {
        // 获取动态转发的用户uid
        Set<Long> uids = queryApi(null, 0, Sets.newHashSet());
        // 打印一下
        Console.log("转发了动态的uid: " + uids);

        JSONObject uidAndName = JSON.parseObject(body);
        Set<String> keySet = uidAndName.keySet();
        Set<Long> uids = keySet.stream().map(Long::parseLong).collect(Collectors.toSet());
        Console.log("抽奖uid数: {}", uids.size());
        Console.log("抽奖时间: {}", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        Console.log("==========================");
        // 抽奖
        Console.log("抽大奖1位开始:");
        roll(uids, 1, uidAndName);

        Console.log("==========================");
        Console.log("抽参与奖50位开始:");
        roll(uids, 50, uidAndName);
    }

    /**
     * 获取已转发动态用户的uid
     *
     * @param offset      下一页的页码offset
     * @param repostCount 已获取的转发数
     * @param uidSet      已获取的uid
     * @return 已转发动态的uid (接口限制只能获取到500+条)
     */
    private static Set<Long> queryApi(String offset, int repostCount, Set<Long> uidSet) {
        Map<String, Object> paramMap = Maps.newHashMap();
        paramMap.put("dynamic_id", DYNAMIC_ID);
        if (offset != null) {
            paramMap.put("offset", offset);
        }

        String body = HttpRequest.get(DYNAMIC_REPOST_API)
                .form(paramMap)
                .execute()
                .body();

        JSONObject json = JSON.parseObject(body);
        JSONObject data = json.getJSONObject("data");

        Integer hasMore = data.getInteger("has_more");
        JSONArray items = data.getJSONArray("items");
        offset = data.getString("offset");

        repostCount += items.size();
        Console.log("已读取到转发数: " + repostCount);

        for (Object item : items) {
            JSONObject jsonObject = (JSONObject) item;
            JSONObject desc = jsonObject.getJSONObject("desc");
            uidSet.add(desc.getLongValue("uid"));
        }

        Console.log("已读取到uid数: " + uidSet.size());

        if (hasMore != null && hasMore == 1) {
            queryApi(offset, repostCount, uidSet);
        }
        return uidSet;
    }

    /**
     * 抽奖
     *
     * @param uidSet 用户uid
     * @param count  中奖个数
     */
    private static void roll(Set<Long> uidSet, int count, JSONObject uidAndName) {
        int rollCount = 0;
        List<Long> uidList = Lists.newArrayList(uidSet.iterator());

        List<Long> luckyUids = Lists.newArrayList();
        List<Long> suspectUids = Lists.newArrayList();
        List<Long> notFollowUids = Lists.newArrayList();

        while (rollCount < count) {
            int randomIndex = RandomUtil.randomInt(0, uidList.size());
            long uid = uidList.remove(randomIndex);
            // 判断是否有关注
            boolean isFollow = isFollow(uid);
            // 判断是否抽奖号
            boolean isRollBot = isRollBot(uid, 7);

            if (isFollow && !isRollBot) {
                Console.log("抽到uid: {}, 有效", uid);
                luckyUids.add(uid);
                ++rollCount;
            }
            if (isRollBot) {
                suspectUids.add(uid);
            }
            if (!isFollow) {
                notFollowUids.add(uid);
            }
        }

        Console.log("==========================");

        Console.log("抽到但并未关注的名单:");
        for (Long uid : notFollowUids) {
            Console.log("uid: {}  昵称: {}", uid, uidAndName.get(uid.toString()));
        }
        Console.log("合计 {} 个", notFollowUids.size());

        Console.log("==========================");

        //Console.log("嫌疑uid: " + suspectUids); // 指的是被判断为抽奖号的uid
        Console.log("抽到但被判为抽奖号名单:");
        for (Long uid : suspectUids) {
            Console.log("uid: {}  昵称: {}", uid, uidAndName.get(uid.toString()));
        }
        Console.log("合计 {} 个", suspectUids.size());

        Console.log("==========================");

        //Console.log("中奖uid: " + luckyUids);
        Console.log("中奖名单:");
        for (Long uid : luckyUids) {
            Console.log("uid: {}  昵称: {}", uid, uidAndName.get(uid.toString()));
        }
        Console.log("合计 {} 个", luckyUids.size());
    }

    /**
     * 判断用户是否是抽奖号
     *
     * @param uid       用户uid
     * @param condition 判断阈值
     * @return
     */
    private static Boolean isRollBot(long uid, int condition) {
        String api = String.format(USER_DYNAMIC_API, uid);

        String body = HttpRequest.get(api)
                .execute()
                .body();

        if (StringUtils.isBlank(body)) {
            Console.log("isRollBot异常, 接口未返回内容. uid:{} body:{}", uid, body);
            return true;
        }

        JSONObject json = JSON.parseObject(body);

        Integer code = json.getInteger("code");
        if (code == null || code != 0) {
            Console.log("isRollBot接口状态码异常, code:{} uid:{} body:{}", code, uid, body);
            return true;
        }

        JSONObject data = json.getJSONObject("data");
        JSONArray cards = data.getJSONArray("cards");
        if (ObjectUtil.isEmpty(cards)) {
            Console.log("uid:{} 的动态为空", uid);
            return true;
        }

        int loopCount = Math.min(cards.size(), 10);
        int rollCount = 0;

        for (int i = 0; i < loopCount; i++) {
            String card = cards.getJSONObject(i).getString("card");
            if (card.contains("抽奖")) {
                ++rollCount;
            }
        }

        if (rollCount >= condition) {
            Console.log("抽到uid: {}, 但被判定为抽奖号 (阈值: {}/{})", uid, rollCount, condition);
            return true;
        }

        return false;
    }

    private static Boolean isFollow(long uid) {
        String api = String.format(CHECK_FOLLOW_API, uid);

        String body = HttpRequest.get(api)
                .cookie(COOKIE)
                .execute()
                .body();

        JSONObject json = JSON.parseObject(body);

        Integer code = json.getInteger("code");
        if (code == null || code != 0) {
            Console.log("isFollow接口状态码异常, code:{} uid:{} body:{}", code, uid, body);
            return true;
        }

        int attribute = json.getJSONObject("data").getJSONObject("be_relation").getIntValue("attribute");

        switch (attribute) {
            case 1:
                // 悄悄关注
            case 2:
                // 关注
            case 6:
                // 互相关注
                return true;
            default:
                Console.log("抽到uid: {}, 但未关注账号", uid);
                return false;
        }
    }
}
