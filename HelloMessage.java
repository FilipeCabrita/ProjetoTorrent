public class HelloMessage extends Message {
        private String ipAddress;
        private int port;
    
        public HelloMessage(String ipAddress, int port) {
            super("HELLO:" + ipAddress + ":" + port, "HELLO");
            this.ipAddress = ipAddress;
            this.port = port;
        }
    
        public String getIpAddress() {
            return ipAddress;
        }
    
        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "HELLO:" + ipAddress + ":" + port;
        }
        
    }