/* Copyright (c) 2017 Cheese and Bacon Games, LLC */
/* This file is licensed under the MIT License. */
/* See the file development/LICENSE.txt for the full license text. */

package org.cheeseandbacon.bitbeast;

public class Food_Category{
	static final int BEGIN=0;
	static final int NONE=BEGIN;
	static final int FRUITS=1;
	static final int VEGETABLES=2;
	static final int MEATS=3;
	static final int BREADS=4;
	static final int DAIRY=5;
	//UNUSED
	static final int JUNK=6;
	static final int END=7;

	public static int string_to_category(String string){
		if(string.equals("none")){
			return NONE;
		}
		else if(string.equals("fruits")){
			return FRUITS;
		}
		else if(string.equals("vegetables")){
			return VEGETABLES;
		}
		else if(string.equals("meats")){
			return MEATS;
		}
		else if(string.equals("breads")){
			return BREADS;
		}
		else if(string.equals("dairy")){
			return DAIRY;
		}
		else if(string.equals("junk")){
			return JUNK;
		}
		else{
			return NONE;
		}
	}

	public static String category_to_string(int category){
		if(category == NONE){
			return "none";
		}
		else if(category == FRUITS){
			return "fruits";
		}
		else if(category == VEGETABLES){
			return "vegetables";
		}
		else if(category == MEATS){
			return "meats";
		}
		else if(category == BREADS){
			return "breads";
		}
		else if(category == DAIRY){
			return "dairy";
		}
		else if(category == JUNK){
			return "junk";
		}
		else{
			return "";
		}
	}
}
