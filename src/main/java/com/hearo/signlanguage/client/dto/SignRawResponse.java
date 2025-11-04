package com.hearo.signlanguage.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class SignRawResponse {
    private Response response;

    @Data public static class Response {
        private Header header;
        private Body body;
    }
    @Data public static class Header {
        private String resultCode; // "0000"
        private String resultMsg;  // "OK"
    }
    @Data public static class Body {
        private Items items;
        private String numOfRows;
        private String pageNo;
        private String totalCount;
    }
    @Data public static class Items {
        private List<Item> item;
    }
    @Data public static class Item {
        private String title;
        private String alternativeTitle;
        private String description;
        private String subDescription;   // mp4 URL
        private String localId;
        private String viewCount;
        private String url;              // 외부 상세
        private String imageObject;      // 썸네일
        private String period;
        private String signDescription;
        private String signImages;       // csv
        private String collectionDb;
        private String categoryType;
    }
}
