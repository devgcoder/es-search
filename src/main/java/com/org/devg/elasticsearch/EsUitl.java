package com.org.devg.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author devg
 * @Date 2019/10/13 17:44
 */
public class EsUitl {

   /**
   * 时间转化成 String 格式的时间
   *
   * @param dateFormat
   * @param millSec
   * @return
   */
  public static String transferLongToDate(String dateFormat, Long millSec) {
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    String millSecStr = millSec + "000";
    Date date = new Date(Long.valueOf(millSecStr));
    String resDate = sdf.format(date);
    return resDate;
  }


}
