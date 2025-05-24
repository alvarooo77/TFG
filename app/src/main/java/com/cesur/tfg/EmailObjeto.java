package com.cesur.tfg;

import java.io.Serializable;

public class EmailObjeto implements Serializable {
    String usuario;
    String asunto;
    String fecha;

    private static final long serialVersionUID = 1L;

    public EmailObjeto(String usuario, String asunto, String fecha) {
        this.usuario = usuario;
        this.asunto = asunto;
        this.fecha = fecha;
    }

    public String getUsuario(){
        return usuario;
    }

    public String getAsunto(){
        return asunto;
    }

    public String getFecha(){
        return fecha;
    }

}


