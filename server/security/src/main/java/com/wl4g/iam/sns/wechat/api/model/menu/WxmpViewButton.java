package com.wl4g.iam.sns.wechat.api.model.menu;

import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;

/**
 * {@link WxmpViewButton}
 *
 * @author James Wong <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2017-08-07
 * @since
 */
public class WxmpViewButton extends WxmpButton {

    private WxButtonType type; // Menu type
    private String url; // Menu click link

    public WxButtonType getType() {
        return type;
    }

    public void setType(WxButtonType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName().concat(" - ").concat(toJSONString(this));
    }

}
