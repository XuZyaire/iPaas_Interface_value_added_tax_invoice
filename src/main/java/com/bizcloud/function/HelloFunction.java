package com.bizcloud.function;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.aip.contentcensor.EImgType;
import com.baidu.aip.ocr.AipOcr;

import com.bizcloud.ipaas.t97f51c2f74cf4b30bced948079b4d0fd.d20210407113259.auth.extension.AuthConfig;
import com.bizcloud.ipaas.t97f51c2f74cf4b30bced948079b4d0fd.d20210407113259.codegen.TclsqingApi;
import com.bizcloud.ipaas.t97f51c2f74cf4b30bced948079b4d0fd.d20210407113259.codegen.TfymxiApi;
import com.bizcloud.ipaas.t97f51c2f74cf4b30bced948079b4d0fd.d20210407113259.model.*;

import com.google.gson.Gson;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HelloFunction {
    //设置APPID/AK/SK
    public static final String APP_ID = "23912083";
    public static final String API_KEY = "FGtUDEUfQOsvVojtOBg6GUrC";
    public static final String SECRET_KEY = "DFRnIRqtNrfpi3jgOMTvVPMVSPZg0zKq";
    //差旅申请
    public static TclsqingApi tclsqingApi = new TclsqingApi();
    //差旅明细
    public static TfymxiApi tfymxiApi = new TfymxiApi();
    public static Gson gson = new Gson();

    public Object handle(Object param, Map<String, String> variables) {
        /* 获取ACCESS 权限 */
        AuthConfig authConfig = new AuthConfig(variables.get("APAAS_ACCESS_KEY"), variables.get("APAAS_ACCESS_SECRET"));
        authConfig.initAuth();
        //初始化AipOcr
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();

        try {
            //获取传入参数
            String inClass = JSONObject.toJSONString(param);

            JSONObject json = JSONObject.parseObject(inClass);
            //获取对象
            Object tClsQing = json.get("t_CLSQing");
            String t_CLSQing = JSONObject.toJSONString(json.get("t_CLSQing"));
            JSONObject json_tClsQing = JSONObject.parseObject(t_CLSQing);
            Object id = json_tClsQing.get("id");
//            System.out.println(id);
            //对象转json数据
            JSONObject jsonData = JSONObject.parseObject(t_CLSQing);

            //调用API接口中的方法查询数据
            TCLSQingDTO clsq_query = new TCLSQingDTO();
            clsq_query.setId(jsonData.get("id").toString());
            List<TCLSQingDTOResponse> list = tclsqingApi.findtCLSQingUsingPOST(clsq_query).getData();
            //将list转为json格式
            String s = gson.toJson(list.get(0));

            //获取附件内容
            JSONObject jsonList = JSONObject.parseObject(s);
            String SCFP = JSONObject.toJSONString(jsonList.get("SCFP"));

            //获取差旅申请下所有明细的单号
            List list_dHao = getDHao(jsonData.get("id").toString());

            //上传附件集合
            List list_SCFP = JSONObject.parseArray(SCFP);
            //循环遍历附件信息
            for (int i = 0; i < list_SCFP.size(); i++) {
                //差旅费用明细
                TFYMXiDTOUpdate update = new TFYMXiDTOUpdate();
                //获取差旅费用信息
                String data_SCFP = gson.toJson(list_SCFP.get(i));
                JSONObject json_data_SCFP = JSONObject.parseObject(data_SCFP);
                //获取其下图片地址
                String filePath = JSONObject.toJSONString(json_data_SCFP.get("filePath"));
                //去掉首尾符号
                String img_url = filePath.substring(1, filePath.length() - 1);
                org.json.JSONObject res = client.vatInvoice(img_url, EImgType.URL, options);

                //获取识别结果
                String words_result = res.get("words_result").toString();
                JSONObject json_words_result = JSONObject.parseObject(words_result);

                //获取发票号码
                String invoiceNum = json_words_result.get("InvoiceNum").toString();

                System.out.println(invoiceNum);

                //判断是否重复单号
                boolean ifRepeat = ifRepeat(list_dHao, invoiceNum);

                System.out.println(ifRepeat);

                if (ifRepeat) {
                    //不重复
                    //获取开单时间
                    String invoiceDate = json_words_result.get("InvoiceDate").toString();
                    String date = invoiceDate.substring(0, invoiceDate.length() - 1);
                    String date_year = date.replace("年", "-");
                    String date_result = date_year.replace("月", "-");

                    //获取货品名称
                    String commodityName = json_words_result.get("CommodityName").toString();
                    List list_commodityName = JSONObject.parseArray(commodityName);
                    //获取规格型号
                    String commodityType = json_words_result.get("CommodityType").toString();
                    List list_commdityType = JSONObject.parseArray(commodityType);
                    //获取单位
                    String commodityUnit = json_words_result.get("CommodityUnit").toString();
                    List list_commdityUnit = JSONObject.parseArray(commodityUnit);
                    //获取数量
                    String commodityNum = json_words_result.get("CommodityNum").toString();
                    List list_commdityNum = JSONObject.parseArray(commodityNum);
                    //获取单价
                    String commodityPrice = json_words_result.get("CommodityPrice").toString();
                    List list_commdityPrice = JSONObject.parseArray(commodityPrice);
                    //获取金额
                    String commodityAmount = json_words_result.get("CommodityAmount").toString();
                    List list_commdityAmount = JSONObject.parseArray(commodityAmount);
                    //获取税额
                    String commodityTax = json_words_result.get("CommodityTax").toString();
                    List list_commodityTax = JSONObject.parseArray(commodityTax);
                    //循环添加
                    //需要进行集合长度的
                    for (int j = 0; j < list_commodityName.size(); j++) {
                        //保存信息
                        SaveOrUpdatetFYMXiDTO saveOrUpdatetFYMXiDTO = new SaveOrUpdatetFYMXiDTO();
                        //货品名称
                        if (list_commodityName.size() > 0) {
                            String commdityName_word = getInfo(list_commodityName, j);
//                        System.out.println(commdityName_word);
                            update.setName(commdityName_word);
                        }
                        //规格型号
                        if (list_commdityType.size() > 0) {
                            String commdityType_word = getInfo(list_commdityType, j);
//                        System.out.println(commdityType_word);
                            update.setGgLXing(commdityType_word);
                        }

                        //单位
                        if (list_commdityUnit.size() > 0) {
                            String commdityUnit_word = getInfo(list_commdityUnit, j);
//                        System.out.println(commdityUnit_word);
                            update.setDwei(commdityUnit_word);
                        }

                        //数量
                        if (list_commdityNum.size() > 0) {
                            String commdityNum_word = getInfo(list_commdityNum, j);
//                        System.out.println(commdityNum_word);
                            update.setSliang(Integer.valueOf(commdityNum_word));
                        }

                        //单价
                        if (list_commdityPrice.size() > 0) {
                            BigDecimal commdityPrice_word = new BigDecimal(getInfo(list_commdityPrice, j));
//                        System.out.println(commdityPrice_word);
                            update.setDjia(commdityPrice_word);
                        }

                        //金额
                        if (list_commdityAmount.size() > 0) {
                            BigDecimal commdityAmount_word = new BigDecimal(getInfo(list_commdityAmount, j));
//                        System.out.println(commdityAmount_word);
                            update.setJinE(commdityAmount_word);
                        }
                        //税额
                        if (list_commodityTax.size()>0){
                            BigDecimal commodtiyTax_word = new BigDecimal(getInfo(list_commodityTax,j));
                            update.setShuiE(commodtiyTax_word);
                        }
                        //设置差旅申请主从
                        update.setClSQing(id);
                        //设置差旅明细单号
                        update.setDhao(invoiceNum);
                        //设置时间
//                    System.out.println(date_result);
                        update.setFsRQi(date_result);
                        //保存信息
                        saveOrUpdatetFYMXiDTO.setUpdate(update);
                        TFYMXiSaveOrUpdateDataResponseObject responseObject = tfymxiApi.saveOrUpdatetFYMXiUsingPOST(saveOrUpdatetFYMXiDTO);
                        System.out.println(responseObject.getMessage());
                    }
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据信息集合获取其中信息
     *
     * @param list
     * @param j
     * @return
     */
    private String getInfo(List list, int j) {
        String data_list = gson.toJson(list.get(j));
        JSONObject json_list = JSONObject.parseObject(data_list);
        return json_list.get("word").toString();
    }

    /**
     * 查询对应差旅申请的所有差旅明细单号
     *
     * @param id
     * @return
     */
    public static List getDHao(String id) {
        //创建存放单号的集合
        List list_Dhao = new ArrayList();
        //定位查询同一个差旅申请下的所有差旅明细
        TFYMXiDTO query = new TFYMXiDTO();
        query.setClSQing(id);
        DataResponseListtFYMXiDTO response = tfymxiApi.findtFYMXiUsingPOST(query);
        List<TFYMXiDTOResponse> responseData = response.getData();
        //循环获取值
        for (int i = 0; i < responseData.size(); i++) {
            String data = gson.toJson(responseData.get(i));
            JSONObject json_data = JSONObject.parseObject(data);

            System.out.println(json_data);

            //获取其中的单号
            String dHao = json_data.get("DHao").toString();

            System.out.println(dHao);

            //存放到集合中
            list_Dhao.add(dHao);
        }
        return list_Dhao;
    }

    /**
     * 判断是否重复
     *
     * @return
     */
    public static boolean ifRepeat(List list_dHao, String invoiceNum) {
        Boolean flag = true;
        for (int i = 0; i < list_dHao.size(); i++) {
            if (list_dHao.get(i).equals(invoiceNum)) {
                //重复
                flag = false;
                break;
            } else {
                //不重复
                flag = true;
                continue;
            }
        }
        //默认不重复
        return flag;
    }

}
