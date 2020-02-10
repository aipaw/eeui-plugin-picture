package app.eeui.picture.ui.entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.luck.picture.lib.PictureSelectionModel;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.R;
import com.luck.picture.lib.compress.Luban;
import com.luck.picture.lib.compress.OnCompressListener;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.eeui.framework.activity.PageActivity;
import app.eeui.framework.extend.annotation.ModuleEntry;
import app.eeui.framework.extend.bean.PageBean;
import app.eeui.framework.extend.bean.WebCallBean;
import app.eeui.framework.extend.module.eeuiJson;
import app.eeui.framework.extend.module.eeuiMap;
import app.eeui.framework.extend.module.eeuiPage;
import app.eeui.framework.extend.module.eeuiParse;
import app.eeui.picture.ui.module.WebModule;
import app.eeui.picture.ui.module.WeexModule;

import static android.app.Activity.RESULT_OK;

@ModuleEntry
public class eeui_picture {

    /**
     * ModuleEntry
     * @param content
     */
    public void init(Context content) {
        try {
            WXSDKEngine.registerModule("eeuiPicture", WeexModule.class);
        } catch (WXException e) {
            e.printStackTrace();
        }
        WebCallBean.addClassData("eeuiPicture", WebModule.class);
    }

    /****************************************************************************************/
    /****************************************************************************************/
    /****************************************************************************************/

    private List<LocalMedia> toLocalMedia(JSONArray selectedList) {
        List<LocalMedia> selected = new ArrayList<>();
        if (selectedList != null) {
            for (int i = 0; i <  selectedList.size(); i++) {
                JSONObject tempJson = eeuiJson.parseObject(selectedList.get(i));
                LocalMedia tempMedia = new LocalMedia();
                tempMedia.setDuration(eeuiJson.getInt(tempJson, "duration"));
                tempMedia.setPath(eeuiJson.getString(tempJson, "path"));
                tempMedia.setCut(eeuiJson.getBoolean(tempJson, "cut"));
                tempMedia.setNum(eeuiJson.getInt(tempJson, "num"));
                tempMedia.setWidth(eeuiJson.getInt(tempJson, "width"));
                tempMedia.setHeight(eeuiJson.getInt(tempJson, "height"));
                tempMedia.setChecked(eeuiJson.getBoolean(tempJson, "checked"));
                tempMedia.setMimeType(eeuiJson.getInt(tempJson, "mimeType"));
                tempMedia.setPosition(eeuiJson.getInt(tempJson, "position"));
                tempMedia.setCompressed(eeuiJson.getBoolean(tempJson, "compressed"));
                tempMedia.setPictureType(eeuiJson.getString(tempJson, "pictureType"));
                selected.add(tempMedia);
            }
        }
        return selected;
    }

    /***************************************************************************************************/
    /***************************************************************************************************/
    /***************************************************************************************************/

    /**
     * 打开相册
     * @param object
     * @param callback
     */
    public void create(Context context, String object, final JSCallback callback) {
        final JSONObject json = eeuiJson.parseObject(object);
        //
        PageActivity.startTransparentPage(context, new JSCallback() {

            @Override
            public void invoke(Object data) {
                if (callback != null) {
                    callback.invoke(data);
                }
            }

            @Override
            public void invokeAndKeepAlive(Object data) {
                Map<String, Object> retData = eeuiMap.objectToMap(data);
                String pageName = eeuiParse.parseStr(retData.get("pageName"));
                String status = eeuiParse.parseStr(retData.get("status"));
                PageBean mBean = eeuiPage.getPageBean(pageName);
                if (mBean == null) {
                    return;
                }
                switch (status) {
                    case "create":
                        List<LocalMedia> selected = toLocalMedia(eeuiJson.parseArray(json.getString("selected")));
                        PictureSelectionModel model;
                        if (eeuiJson.getString(json, "type", "gallery").equals("camera")) {
                            model = PictureSelector.create(mBean.getActivity())
                                    .openCamera(eeuiJson.getInt(json, "gallery", PictureMimeType.ofAll())); // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        }else{
                            model = PictureSelector.create(mBean.getActivity())
                                    .openGallery(eeuiJson.getInt(json, "gallery", PictureMimeType.ofAll())); // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        }
                        model.maxSelectNum(eeuiJson.getInt(json, "maxNum", 9))                         // 最大选择数量 int
                                .minSelectNum(eeuiJson.getInt(json, "minNum", 0))                      // 最小选择数量 int
                                .imageSpanCount(eeuiJson.getInt(json, "spanCount", 4))                 // 每行显示个数 int
                                .selectionMode(eeuiJson.getInt(json, "mode", PictureConfig.MULTIPLE))  // 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                                .previewImage(eeuiJson.getBoolean(json, "previewImage", true))         // 是否可预览图片 true or false
                                .previewVideo(eeuiJson.getBoolean(json, "previewVideo", true))         // 是否可预览视频 true or false
                                .enablePreviewAudio(eeuiJson.getBoolean(json, "previewAudio", true))   // 是否可播放音频 true or false
                                .isCamera(eeuiJson.getBoolean(json, "camera", true))                   // 是否显示拍照按钮 true or false
                                .imageFormat(eeuiJson.getString(json, "format", PictureMimeType.JPEG)) // 拍照保存图片格式后缀,默认jpeg
                                .isZoomAnim(eeuiJson.getBoolean(json, "zoomAnim", true))               // 图片列表点击 缩放效果 默认true
                                .sizeMultiplier(eeuiJson.getFloat(json, "multiplier", 0.5f))           // glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                                .enableCrop(eeuiJson.getBoolean(json, "crop", false))                  // 是否裁剪 true or false
                                .compress(eeuiJson.getBoolean(json, "compress", false))                // 是否压缩 true or false
                                .glideOverride(eeuiJson.getInt(json, "overrideWidth", 100), eeuiJson.getInt(json, "overrideHeight", 100))     // int glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                                .withAspectRatio(eeuiJson.getInt(json, "ratioX", 1), eeuiJson.getInt(json, "ratioY", 1))                      // int 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
                                .hideBottomControls(eeuiJson.getBoolean(json, "cropControls", false))  // 是否显示uCrop工具栏，默认不显示 true or false
                                .isGif(eeuiJson.getBoolean(json, "gif", false))                        // 是否显示gif图片 true or false
                                .freeStyleCropEnabled(eeuiJson.getBoolean(json, "freeCrop", false))    // 裁剪框是否可拖拽 true or false
                                .circleDimmedLayer(eeuiJson.getBoolean(json, "circle", false))         // 是否圆形裁剪 true or false
                                .showCropFrame(eeuiJson.getBoolean(json, "cropFrame", true))           // 是否显示裁剪矩形边框 圆形裁剪时建议设为false   true or false
                                .showCropGrid(eeuiJson.getBoolean(json, "cropGrid", true))             // 是否显示裁剪矩形网格 圆形裁剪时建议设为false    true or false
                                .openClickSound(eeuiJson.getBoolean(json, "clickSound", false))        // 是否开启点击声音 true or false
                                .selectionMedia(selected)                                               // 是否传入已选图片 List<LocalMedia> list
                                .previewEggs(eeuiJson.getBoolean(json, "eggs", false))                 // 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中) true or false
                                .cropCompressQuality(eeuiJson.getInt(json, "quality", 90))             // 裁剪压缩质量 默认90 int
                                .minimumCompressSize(eeuiJson.getInt(json, "compressSize", 100))       // 小于100kb的图片不压缩
                                .synOrAsy(eeuiJson.getBoolean(json, "sync", true))                     // 同步true或异步false 压缩 默认同步
                                .cropWH(eeuiJson.getInt(json, "cropWidth", 0), eeuiJson.getInt(json, "cropHeight", 0))                        // 裁剪宽高比，设置如果大于图片本身宽高则无效 int
                                .rotateEnabled(eeuiJson.getBoolean(json, "rotate", true))              // 裁剪是否可旋转图片 true or false
                                .scaleEnabled(eeuiJson.getBoolean(json, "scale", true))                // 裁剪是否可放大缩小图片 true or false
                                .videoQuality(eeuiJson.getInt(json, "videoQuality", 0))                // 视频录制质量 0 or 1 int
                                .videoMaxSecond(eeuiJson.getInt(json, "videoMaxSecond", 15))           // 显示多少秒以内的视频or音频也可适用 int
                                .videoMinSecond(eeuiJson.getInt(json, "videoMinSecond", 10))           // 显示多少秒以内的视频or音频也可适用 int
                                .recordVideoSecond(eeuiJson.getInt(json, "recordVideoSecond", 60))     // 视频秒数录制 默认60s int
                                .forResult(PictureConfig.CHOOSE_REQUEST);
                        break;

                    case "activityResult":
                        int requestCode = eeuiParse.parseInt(retData.get("requestCode"));
                        int resultCode = eeuiParse.parseInt(retData.get("resultCode"));
                        if (resultCode == RESULT_OK) {
                            switch (requestCode) {
                                case PictureConfig.CHOOSE_REQUEST:
                                    if (callback != null) {
                                        Map<String, Object> callData = new HashMap<>();
                                        callData.put("status", "success");
                                        callData.put("lists", PictureSelector.obtainMultipleResult((Intent) retData.get("resultData")));
                                        callback.invokeAndKeepAlive(callData);
                                    }
                                    break;
                            }
                        }
                        mBean.getActivity().finish();
                        break;
                }
                if (callback != null) {
                    callback.invokeAndKeepAlive(data);
                }
            }
        });
    }

    /**
     * 压缩图片
     * @param object
     * @param callback
     */
    public void compressImage(Context context, String object, final JSCallback callback) {
        JSONObject json = eeuiJson.parseObject(object);
        final List<LocalMedia> selected = toLocalMedia(eeuiJson.parseArray(json.getString("lists")));
        Luban.with(context)
                .loadMediaData(selected)
                .ignoreBy(eeuiJson.getInt(json, "compressSize", 100))
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(List<LocalMedia> list) {
                        RxBus.getDefault().post(new EventEntity(PictureConfig.CLOSE_PREVIEW_FLAG));
                        if (callback != null) {
                            Map<String, Object> callData = new HashMap<>();
                            callData.put("status", "success");
                            callData.put("lists", list);
                            callback.invokeAndKeepAlive(callData);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        RxBus.getDefault().post(new EventEntity(PictureConfig.CLOSE_PREVIEW_FLAG));
                        if (callback != null) {
                            Map<String, Object> callData = new HashMap<>();
                            callData.put("status", "error");
                            callData.put("lists", selected);
                            callback.invokeAndKeepAlive(callData);
                        }
                    }
                }).launch();
    }

    /**
     * 预览图片
     * @param position
     * @param array
     */
    public void picturePreview(Context context, int position, String array, JSCallback callback) {
        JSONArray lists = eeuiJson.parseArray(array);
        if (lists.size() == 0) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("path", array);
            lists.add(tempJson);
        }
        //
        List<LocalMedia> mediaLists = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            LocalMedia tempMedia = new LocalMedia();
            if (lists.get(i) instanceof String) {
                tempMedia.setPath((String) lists.get(i));
            }else{
                JSONObject tempJson = eeuiJson.parseObject(lists.get(i));
                tempMedia.setPath(tempJson.getString("path"));
            }
            mediaLists.add(tempMedia);
        }
        if (mediaLists.size() == 0) {
            return;
        }
        PictureSelector.create((Activity) context).themeStyle(R.style.picture_default_style).openExternalPreview(position, mediaLists, callback);
    }

    /**
     * 预览视频
     * @param path
     */
    public void videoPreview(Context context, String path) {
        PictureSelector.create((Activity) context).externalPictureVideo(path);
    }

    /**
     * 缓存清除，包括裁剪和压缩后的缓存，要在上传成功后调用，注意：需要系统sd卡权限
     */
    public void deleteCache(Context context) {
        PictureFileUtils.deleteCacheDirFile(context);
    }

}
