package com.pablo67340.guishop.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author Bryce
 */
@Data
public class MenuItem implements Cloneable {

    Map<String, MenuPage> pages = new LinkedHashMap<>();
}
