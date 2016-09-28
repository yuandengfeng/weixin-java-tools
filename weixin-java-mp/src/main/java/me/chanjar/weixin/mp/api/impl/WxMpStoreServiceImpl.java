package me.chanjar.weixin.mp.api.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;

import org.joor.Reflect;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import me.chanjar.weixin.common.annotation.Required;
import me.chanjar.weixin.common.bean.result.WxError;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.WxMpStoreService;
import me.chanjar.weixin.mp.bean.store.WxMpStoreBaseInfo;
import me.chanjar.weixin.mp.bean.store.WxMpStoreInfo;
import me.chanjar.weixin.mp.bean.store.WxMpStoreListResult;

/**
 *  Created by Binary Wang on 2016/9/26.
 * @author binarywang (https://github.com/binarywang)
 *
 */
public class WxMpStoreServiceImpl implements WxMpStoreService {
  private static final String API_BASE_URL = "http://api.weixin.qq.com/cgi-bin/poi";

  private WxMpService wxMpService;

  public WxMpStoreServiceImpl(WxMpService wxMpService) {
    this.wxMpService = wxMpService;
  }

  @Override
  public void add(WxMpStoreBaseInfo request) throws WxErrorException {
    checkParameters(request);

    String url = API_BASE_URL + "/addpoi";
    String response = this.wxMpService.post(url, request.toJson());
    WxError wxError = WxError.fromJson(response);
    if (wxError.getErrorCode() != 0) {
      throw new WxErrorException(wxError);
    }
  }

  private void checkParameters(WxMpStoreBaseInfo request) {
    List<String> nullFields = Lists.newArrayList();
    for (Entry<String, Reflect> entry : Reflect.on(request).fields()
        .entrySet()) {
      Reflect reflect = entry.getValue();
      try {
        Field field = request.getClass().getDeclaredField(entry.getKey());
        if (field.isAnnotationPresent(Required.class)
            && reflect.get() == null) {
          nullFields.add(entry.getKey());
        }
      } catch (NoSuchFieldException | SecurityException e) {
        e.printStackTrace();
      }
    }

    if (!nullFields.isEmpty()) {
      throw new IllegalArgumentException("必填字段[" + nullFields + "]必须提供值");
    }

  }

  @Override
  public WxMpStoreListResult list(int begin, int limit)
      throws WxErrorException {
    String url = API_BASE_URL + "/getpoilist";
    JsonObject params = new JsonObject();
    params.addProperty("begin", begin);
    params.addProperty("limit", limit);
    String response = this.wxMpService.post(url, params.toString());

    WxError wxError = WxError.fromJson(response);
    if (wxError.getErrorCode() != 0) {
      throw new WxErrorException(wxError);
    }

    return WxMpStoreListResult.fromJson(response);
  }

  @Override
  public List<WxMpStoreInfo> listAll() throws WxErrorException {
    int limit = 50;
    WxMpStoreListResult list = this.list(0, limit);
    List<WxMpStoreInfo> stores = list.getBusinessList();
    if (list.getTotalCount() > limit) {
      int begin = limit;
      WxMpStoreListResult followingList = this.list(begin, limit);
      while (followingList.getBusinessList().size() > 0) {
        stores.addAll(followingList.getBusinessList());
        begin += limit;
        if (begin >= list.getTotalCount()) {
          break;
        }
        followingList = this.list(begin, limit);
      }
    }

    return stores;
  }

}
