package global.model;

import java.io.Serializable;

/**
 * Created by Maximilian on 12.01.2017.
 */
public interface IRegistrationRequest extends Serializable {

    public String getIP();

    public String getID();

}