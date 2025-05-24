package com.cesur.tfg;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder>{

    ArrayList<EmailObjeto> listDatos;

    public interface OnItemClickListener{
        void onItemClick(EmailObjeto email, int position);
    }

    //constructor
    public EmailAdapter(ArrayList<EmailObjeto> listDatos) {
        if(listDatos != null){
            this.listDatos = listDatos;
        }else{
            this.listDatos = new ArrayList<EmailObjeto>();
        }

    }

    public void updateData(ArrayList<EmailObjeto> nuevosEmails) {
        if (nuevosEmails == null) {
            this.listDatos.clear();
            Log.w("EmailAdapter", "updateData recibió una lista nula. Limpiando datos existentes.");
        } else {
            this.listDatos.clear();
            this.listDatos.addAll(nuevosEmails);
            Log.d("EmailAdapter", "Datos actualizados. Nueva cantidad de emails: " + this.listDatos.size());
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new EmailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailAdapter.EmailViewHolder holder, int position) {

        // Control de errores
        if(listDatos != null && position < listDatos.size()){
            EmailObjeto email = listDatos.get(position);
            holder.asignarDatos(listDatos.get(position));
        }else {
            Log.e("EmailAdapter", "posicion invalida o listDatos es nulo en este momento");
        }

    }

    @Override
    public int getItemCount() {
        if (listDatos != null) {
            return listDatos.size();
        } else {
            return 0;
        }
    }

    public class EmailViewHolder extends RecyclerView.ViewHolder{
        TextView usuarioC;
        TextView asuntoC;
        TextView fechaC;

        public EmailViewHolder(@NonNull View itemView) {
            super(itemView);
            usuarioC = itemView.findViewById(R.id.usuario);
            asuntoC = itemView.findViewById(R.id.asunto);
            fechaC = itemView.findViewById(R.id.fecha);
        }

        public void asignarDatos(EmailObjeto email){
            if(email != null){
                Log.d("EmailViewHolder", "Binding email: " + email.getAsunto());
                this.usuarioC.setText(email.getUsuario());
                this.asuntoC.setText(email.getAsunto());
                this.fechaC.setText(email.getFecha());
            }else{
                Log.w("EmailViewHolder", "Intento de bind con email nulo.");
                this.usuarioC.setText("Error");
                this.asuntoC.setText("Error");
                this.fechaC.setText("Error");
            }
        }
    }


}



