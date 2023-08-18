package com.hbm.inventory.control_panel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbm.inventory.control_panel.controls.*;

public class ControlRegistry {
	
	public static Map<String, Control> registry = new HashMap<>();
	private static Map<Class<? extends Control>, String> classToName = new HashMap<>();

	private ControlRegistry(){
	}
	
	public static void init(){
//		registry.put("button", new Button("Button", null));
//		registry.put("button_covered", new ButtonCovered("Covered Button", null));
		registry.put("button_hazard", new ButtonHazard("Emergency Button", null));
//		registry.put("button_covered", new ButtonCovered("Covered Button", null));
//		registry.put("button_push", new ButtonPush("Push Button", null));

		registry.put("display_7seg", new DisplaySevenSeg("7-seg Display", null));
//		registry.put("indicatorLamp", new IndicatorLamp("Indicator Lamp", null));
//		registry.put("toggleSwitch", new ToggleSwitch("Toggle Switch", null));
//		registry.put("coveredToggleSwitch", new CoveredToggleSwitch("Covered Toggle Switch", null));

		for(Entry<String, Control> e : registry.entrySet()){
			classToName.put(e.getValue().getClass(), e.getKey());
		}
	}
	
	public static List<Control> getAllControls(){
		List<Control> l = new ArrayList<>(registry.size());
		for(Control c : registry.values())
			l.add(c);
		return l;
	}

	public static List<String> getAllControlsOfType(ControlType type) {
		List<String> l = new ArrayList<>();
		for (Entry<String, Control> c : registry.entrySet())
			if (c.getValue().getControlType() == type) {
				l.add(c.getKey());
			}
		return l;
	}

	public static Control getNew(String name, ControlPanel panel){
		return registry.get(name).newControl(panel);
	}

	public static String getName(Class<? extends Control> clazz){
		return classToName.get(clazz);
	}
}
