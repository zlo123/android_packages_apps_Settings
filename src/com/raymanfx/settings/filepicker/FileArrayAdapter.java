/*
 * Copyright (C) 2012 RaymanFX (raymanfx@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raymanfx.settings.filepicker;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.settings.R;

public class FileArrayAdapter extends ArrayAdapter<Option>{

    private Context c;
    private int id;
    private List<Option>items;
    
    public FileArrayAdapter(Context context, int textViewResourceId,
            List<Option> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }
    public Option getItem(int i)
     {
         return items.get(i);
     }
     @Override
       public View getView(int position, View convertView, ViewGroup parent) {
               View v = convertView;
               if (v == null) {
                   LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                   v = vi.inflate(id, null);
               }
               final Option o = items.get(position);
               if (o != null) {
                       TextView t1 = (TextView) v.findViewById(R.id.FilePickerTextView01);
                       TextView t2 = (TextView) v.findViewById(R.id.FilePickerTextView02);
                       
                       if(t1!=null)
                           t1.setText(o.getName());
                       if(t2!=null)
                           t2.setText(o.getData());
                       
               }
               return v;
       }

}

