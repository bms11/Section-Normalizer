public class SectionAndRowName{

		final String rowName, sectionName;
    	
    	public SectionAndRowName(String sectionName, String rowName){
    		this.rowName = rowName;
    		this.sectionName = sectionName;
    	}
    	
    	@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((rowName == null) ? 0 : rowName.hashCode());
			// Making sure hashcode isn't thrown off during comparisons in our manifest 
			// and making sure we don't null out our sectionName field.
			String sect = sectionName;
			sect = Normalizer.findValidSection(sect);
			result = prime * result + ((sect == null) ? 0 : sect.hashCode());
			return result;
		}
    	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SectionAndRowName other = (SectionAndRowName) obj;
			
			String otherSectionNameCopy = other.sectionName,
				   sectionNameCopy = sectionName;

			if (rowName == null) {
				if (other.rowName != null)
					return false;
			} else if (!rowName.equals(other.rowName))
				return false;
			if (sectionNameCopy == null) {
				if (other.sectionName != null)
					return false;
			} else if (!Normalizer.findValidSection(sectionNameCopy).equals(Normalizer.findValidSection(otherSectionNameCopy)))
				return false;
			return true;
		}
		

}